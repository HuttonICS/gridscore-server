package jhi.gridscore.server;

import jhi.gridscore.server.database.Database;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.*;

import static jhi.gridscore.server.database.codegen.tables.Configurations.*;

public class OldConfigRemoverThread implements Runnable
{
	@Override
	public void run()
	{
		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			// Delete configurations that are older than an hour
			context.deleteFrom(CONFIGURATIONS)
				   .where(DSL.abs(timestampDiff(DatePart.DAY, CONFIGURATIONS.CREATED_ON, DSL.now())).ge(30))
				   .execute();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Utility function to use until jOOQ implements this properly
	 *
	 * @param part The {@link DatePart} to use for the comparison
	 * @param t1   The {@link Timestamp} field one
	 * @param t2   The {@link Timestamp} field two
	 * @return The {@link Field} representing the difference in the {@link DatePart} component
	 */
	public static Field<Integer> timestampDiff(DatePart part, Field<Timestamp> t1, Field<Timestamp> t2)
	{
		return DSL.field("timestampdiff({0}, {1}, {2})",
			Integer.class, DSL.keyword(part.toSQL()), t1, t2);
	}
}
