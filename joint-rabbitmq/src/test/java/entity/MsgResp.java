package entity;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

@Embeddable
public class MsgResp implements Serializable, Cloneable {

	public static final int FAILED = 0;
	
	public MsgResp(Integer statusCode, String responseStr) {
		super();
		this.statusCode = statusCode;
		this.responseStr = responseStr;
	}
	
	public MsgResp() {}

	@Basic
	private Integer statusCode = 200;

	public Integer getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}

	@Basic
	@Column(name="responseStr", columnDefinition="TEXT", nullable=true)//mysql
	private String responseStr = "ok";

	@XmlElement
	public String getResponseStr() {
		return responseStr;
	}

	public void setResponseStr(String response) {
		this.responseStr = response;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((responseStr == null) ? 0 : responseStr.hashCode());
		result = prime * result + ((statusCode == null) ? 0 : statusCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MsgResp))
			return false;
		MsgResp other = (MsgResp) obj;
		if (responseStr == null) {
			if (other.responseStr != null)
				return false;
		} else if (!responseStr.equals(other.responseStr))
			return false;
		if (statusCode == null) {
			if (other.statusCode != null)
				return false;
		} else if (!statusCode.equals(other.statusCode))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"Response\", statusCode:").append(statusCode).append(", responseStr:'").append(responseStr).append("'}");
		return builder.toString();
	}
	
	public MsgResp clone() {
		return new MsgResp(statusCode, responseStr);
	}
}