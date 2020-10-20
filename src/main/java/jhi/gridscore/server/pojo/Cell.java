package jhi.gridscore.server.pojo;

import java.util.List;

public class Cell
{
	private String       name;
	private List<String> dates;
	private List<String> values;
	private Geolocation  geolocation;
	private String       comment;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public List<String> getDates()
	{
		return dates;
	}

	public void setDates(List<String> dates)
	{
		this.dates = dates;
	}

	public List<String> getValues()
	{
		return values;
	}

	public void setValues(List<String> values)
	{
		this.values = values;
	}

	public Geolocation getGeolocation()
	{
		return geolocation;
	}

	public void setGeolocation(Geolocation geolocation)
	{
		this.geolocation = geolocation;
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}
}
