package entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.HashSet;
import java.util.Set;

@XmlRootElement
@Entity
@Table(name = "exchange_cfg")
@Cacheable(false)
public class ExchangeCfg extends Configuration {
	private static final long serialVersionUID = 1L;

	public ExchangeCfg() {
		
	}
	
	public ExchangeCfg(String exchangeName, ServerCfg serverCfg) {
		super();
		this.exchangeName = exchangeName;
		this.serverCfg = serverCfg;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id = -1;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Basic
	private String exchangeName;
	@Basic
	private boolean durable = true;
	@Basic
	private boolean autoDelete = false;
	@Basic
	private String type = "direct";

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "server_id")
	private ServerCfg serverCfg;

	@ManyToMany(fetch=FetchType.LAZY, cascade= {CascadeType.PERSIST, CascadeType.REFRESH})
	private Set<QueueCfg> queues = new HashSet<QueueCfg>();

	@XmlTransient
	@JsonIgnoreProperties
	public Set<QueueCfg> getQueues() {
		return queues;
	}

	public void setQueues(Set<QueueCfg> queues) {
		this.queues = queues;
	}

	public ServerCfg getServerCfg() {
		return serverCfg;
	}

	public void setServerCfg(ServerCfg serverCfg) {
		this.serverCfg = serverCfg;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}

	public boolean isDurable() {
		return durable;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public boolean isAutoDelete() {
		return autoDelete;
	}

	public void setAutoDelete(boolean autoDelete) {
		this.autoDelete = autoDelete;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		if (id != -1) {
			return id;
		}
		
		result = prime * result + ((exchangeName == null) ? 0 : exchangeName.hashCode());
		result = prime * result + ((serverCfg == null) ? 0 : serverCfg.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ExchangeCfg))
			return false;
		ExchangeCfg other = (ExchangeCfg) obj;
		
		if (id != -1) {
			return id.equals(other.id);
		}
		
		if (exchangeName == null) {
			if (other.exchangeName != null)
				return false;
		} else if (!exchangeName.equals(other.exchangeName))
			return false;
		if (id != other.id)
			return false;
		if (serverCfg == null) {
			if (other.serverCfg != null)
				return false;
		} else if (!serverCfg.equals(other.serverCfg))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"ExchangeCfg\", id:'").append(id).append("', exchangeName:'").append(exchangeName).append("', durable:'").append(durable).append("', autoDelete:'").append(autoDelete).append("', type:'").append(type).append("}");
		return builder.toString();
	}
	
	@Basic
	private boolean enabled = true;
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
