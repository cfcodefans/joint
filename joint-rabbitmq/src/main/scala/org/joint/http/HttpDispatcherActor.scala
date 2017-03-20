package org.joint.http

import java.io.{IOException, UnsupportedEncodingException}
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util
import java.util.concurrent.Executors
import java.util.function.Consumer

import org.apache.commons.collections4.MapUtils
import org.apache.commons.lang3.time.StopWatch
import org.apache.commons.lang3.{CharEncoding, ObjectUtils, StringUtils}
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.client5.http.impl.sync.{FutureRequestExecutionService, HttpClients}
import org.apache.hc.client5.http.methods.{HttpGet, HttpPost, HttpUriRequest}
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.sync.ResponseHandler
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.entity.EntityUtils
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.http.{HttpResponse, NameValuePair}
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.amqp.entity.{HttpDestinationCfg, MessageContext, MsgResp, QueueCfg}
import org.joint.amqp.manager.MsgMonitor
import org.joints.commons.MiscUtils

import scala.collection.JavaConverters._

/**
  * Created by fan on 2017/2/1.
  */
object HttpDispatcherActor {
    val DEFAULT_TIMEOUT: Int = 30000
    val CLIENT_NUM: Int = MiscUtils.AVAILABLE_PROCESSORS * 5

    //    private val system: ActorSystem = ActorSystem(classOf[HttpDispatcherActor].getSimpleName)
    //    var router: Router = {
    //        val routees: Vector[Routee] = Vector.fill(MiscUtils.AVAILABLE_PROCESSORS) {
    //            val r = system.actorOf(Props[HttpDispatcherActor](new HttpDispatcherActor))
    //            ActorRefRoutee(r)
    //        }
    //        Router(RoundRobinRoutingLogic(), routees)
    //    }

    lazy val instance: HttpDispatcherActor = new HttpDispatcherActor

    val log: Logger = LogManager.getLogger(classOf[HttpDispatcherActor])
    val CHARSET: Charset = Charset.forName(CharEncoding.UTF_8)

    private[http] val DEFAULT_REQ_CFG: RequestConfig = RequestConfig.custom.setConnectTimeout(DEFAULT_TIMEOUT).setSocketTimeout(DEFAULT_TIMEOUT).setConnectTimeout(DEFAULT_TIMEOUT).build
    private[http] val timeoutAndReqCfgs: java.util.Map[Int, RequestConfig] = new util.concurrent.ConcurrentHashMap[Int, RequestConfig]();

    def getParams(nameAndValues: String*): Seq[NameValuePair] = {
        0.until(nameAndValues.size, 2).map(i => new BasicNameValuePair(nameAndValues(i), nameAndValues(i + 1)))
    }

}

class HttpDispatcherActor extends Consumer[MessageContext] {

    import HttpDispatcherActor._

    protected def resolveDestUrl(mc: MessageContext): String = {
        val qc: QueueCfg = mc.getQueueCfg
        val destCfg: HttpDestinationCfg = qc.getDestCfg
        val defaultUrl: String = destCfg.getUrl

        val headers: java.util.Map[String, AnyRef] = mc.getDelivery.getProperties.getHeaders
        if (MapUtils.isEmpty(headers)) return defaultUrl

        val origin = ObjectUtils.defaultIfNull(headers.get("consumer_target"), defaultUrl).toString
        return if (StringUtils.isBlank(origin)) defaultUrl else origin
    }

    override def accept(mc: MessageContext): Unit = {
        val qc: QueueCfg = mc.getQueueCfg
        val destCfg: HttpDestinationCfg = qc.getDestCfg
        val destUrlStr: String = resolveDestUrl(mc)
        var req: HttpUriRequest = null
        val bodyStr: String = new String(mc.getMessageBody)
        try {
            if (!"get".equalsIgnoreCase(StringUtils.trim(destCfg.getHttpMethod))) {
                val post = new HttpPost(destUrlStr)
                //TODO should be queueName instead of config name
                val params: Seq[NameValuePair] = getParams("queueName", qc.getName, "bodyData", bodyStr)
                val fe: UrlEncodedFormEntity = new UrlEncodedFormEntity(params.asJava, CHARSET)
                //				GzipCompressingEntity ze = new GzipCompressingEntity(fe);
                post.setEntity(fe)
                req = post
            } else {
                req = new HttpGet(destUrlStr + "?" + URLEncoder.encode(bodyStr, "UTF-8"))
            }
        } catch {
            case e: UnsupportedEncodingException => {
                log.error("fail to encode message: " + bodyStr, e)
                return mc
            }
        }
        if (StringUtils.isNotBlank(destCfg.getHostHead)) req.addHeader("host", destCfg.getHostHead)
        val httpClientCtx = HttpClientContext.create
        val timeout = Math.max(destCfg.getTimeout, DEFAULT_TIMEOUT).toInt
        httpClientCtx.setRequestConfig(
            timeoutAndReqCfgs.computeIfAbsent(timeout, (timeout: Int) => RequestConfig.custom.setConnectionRequestTimeout(timeout).setSocketTimeout(timeout).setConnectTimeout(timeout).build)
        )

        val clientArray: Array[FutureRequestExecutionService] = closeableHttpClients
        clientArray((mc.getDelivery.getEnvelope.getDeliveryTag % clientArray.length).toInt)
            .execute(req, httpClientCtx, new RespHandler(mc))
    }

    //    override def receive: Receive = {
    //        case (mc: MessageContext) => accept(mc)
    //    }

    private lazy val closeableHttpClients: Array[FutureRequestExecutionService] =
        (0 until CLIENT_NUM toArray).map(i => {
            val ioCfg: IOReactorConfig = IOReactorConfig.custom.setInterestOpQueued(true).build
            val chc = HttpClients.custom
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setMaxConnTotal(MiscUtils.getPropertyNumber("http.client.max.connection", 50).toInt)
                .setMaxConnPerRoute(MiscUtils.getPropertyNumber("http.client.max.connection.per_route", 25).toInt)
                .setDefaultRequestConfig(DEFAULT_REQ_CFG)
                .build
            new FutureRequestExecutionService(chc, Executors.newWorkStealingPool)
        })
}

private[http] class RespHandler(var mc: MessageContext) extends ResponseHandler[HttpResponse] with FutureCallback[HttpResponse] {

    import HttpDispatcherActor._

    private val sw: StopWatch = new StopWatch
    sw.start()

    def cancelled() {
        //        MQueueMgr.instance.acknowledge(mc)
    }

    def completed(resp: HttpResponse) {
        sw.stop()
        var respStr: String = null
        try {
            respStr = EntityUtils.toString(resp.getEntity)
        } catch {
            case e: Exception => {
                log.error("failed to process response from url: \n" + mc.getQueueCfg.getDestCfg.getUrl, e)
                respStr = e.getMessage
            }
        }
        mc.setResponse(new MsgResp(resp.getStatusLine.getStatusCode, StringUtils.substring(StringUtils.trimToEmpty(respStr), 0, 2000)))
        MsgMonitor.prefLog(mc, log)
        if (mc.getResponse.statusCode >= 400 || !mc.isSucceeded) {
            mc.fail
        }
        //TODO responder
    }

    def failed(e: Exception) {
        sw.stop()
        val time = sw.getTime
        val deliveryTag = mc.getDelivery.getEnvelope.getDeliveryTag
        log.error(s"Message: $deliveryTag failed after $time ms \n getting response from url: \n${mc.getQueueCfg.getDestCfg.getUrl}")
        mc.setResponse(new MsgResp(MsgResp.FAILED, s"{status: ${e.getClass.getSimpleName}, resp: '${e.getMessage}', time: $time}"))
        MsgMonitor.prefLog(mc, log)
        mc.fail
        //TODO responder
    }

    @throws[IOException]
    def handleResponse(resp: HttpResponse) = null
}