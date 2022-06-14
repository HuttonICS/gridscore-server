package jhi.gridscore.server.pojo;

import java.sql.Timestamp;

public class MiniConf
{
	private String    uuid;
	private Timestamp lastUpdatedOn;

	public String getUuid()
	{
		return uuid;
	}

	public MiniConf setUuid(String uuid)
	{
		this.uuid = uuid;
		return this;
	}

	public Timestamp getLastUpdatedOn()
	{
		return lastUpdatedOn;
	}

	public MiniConf setLastUpdatedOn(Timestamp lastUpdatedOn)
	{
		this.lastUpdatedOn = lastUpdatedOn;
		return this;
	}

	@Override
	public String toString()
	{
		return "MiniConf{" +
			"uuid='" + uuid + '\'' +
			", lastUpdatedOn=" + lastUpdatedOn +
			'}';
	}
}
