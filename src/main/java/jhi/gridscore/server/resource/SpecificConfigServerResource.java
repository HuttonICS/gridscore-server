package jhi.gridscore.server.resource;

import jhi.gridscore.server.database.Database;
import jhi.gridscore.server.database.codegen.tables.records.ConfigurationsRecord;
import jhi.gridscore.server.pojo.Configuration;
import org.jooq.DSLContext;
import org.jooq.tools.StringUtils;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;

import static jhi.gridscore.server.database.codegen.tables.Configurations.*;

public class SpecificConfigServerResource extends ServerResource
{
	private String id;

	@Override
	protected void doInit()
		throws ResourceException
	{
		super.doInit();

		try
		{
			this.id = getRequestAttributes().get("id").toString();
		}
		catch (NullPointerException | NumberFormatException e)
		{
		}
	}

	@Get
	public Configuration postConfiguration()
	{
		if (StringUtils.isEmpty(id))
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
		else
		{
			try (Connection conn = Database.getConnection();
				 DSLContext context = Database.getContext(conn))
			{
				ConfigurationsRecord record = context.selectFrom(CONFIGURATIONS)
													 .where(CONFIGURATIONS.UUID.eq(id))
													 .fetchAny();

				if (record == null)
				{
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
				}
				else
				{
					Configuration result = record.getConfiguration();
					record.delete();

					return result;
				}
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
			}
		}
	}
}
