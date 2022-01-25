package jhi.gridscore.server.pojo;

public class Trait
{
	private String       name;
	private String       type;
	private String       mType;
	private Restrictions restrictions;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getmType()
	{
		return mType;
	}

	public Trait setmType(String mType)
	{
		this.mType = mType;
		return this;
	}

	public Restrictions getRestrictions()
	{
		return restrictions;
	}

	public void setRestrictions(Restrictions restrictions)
	{
		this.restrictions = restrictions;
	}
}
