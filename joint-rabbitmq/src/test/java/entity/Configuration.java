package entity;

import javax.persistence.Basic;
import javax.persistence.Column;
import java.io.Serializable;

public class Configuration implements Serializable {
	private static final long serialVersionUID = 1L;

	@Basic
	@Column(name = "ver")
	private int version = 0;

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + version;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Configuration))
			return false;
		Configuration other = (Configuration) obj;
		if (version != other.version)
			return false;
		return true;
	}

}
