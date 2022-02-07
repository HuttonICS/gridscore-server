package jhi.gridscore.server.pojo;

public class Settings
{
	private String plausibleDomain;
	private String plausibleApiHost;
	private Boolean plausibleHashMode;

	public Settings()
	{
	}

	public String getPlausibleDomain()
	{
		return plausibleDomain;
	}

	public Settings setPlausibleDomain(String plausibleDomain)
	{
		this.plausibleDomain = plausibleDomain;
		return this;
	}

	public String getPlausibleApiHost()
	{
		return plausibleApiHost;
	}

	public Settings setPlausibleApiHost(String plausibleApiHost)
	{
		this.plausibleApiHost = plausibleApiHost;
		return this;
	}

	public Boolean getPlausibleHashMode()
	{
		return plausibleHashMode;
	}

	public Settings setPlausibleHashMode(Boolean plausibleHashMode)
	{
		this.plausibleHashMode = plausibleHashMode;
		return this;
	}
}
