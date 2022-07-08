import com.google.gson.Gson;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.*;
import jhi.gridscore.server.pojo.*;
import jhi.gridscore.server.pojo.Configuration;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.stream.Collectors;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConsecutiveSingleDeviceUpdateTest extends ConfigTest
{
	private static Configuration trial;
	private static Gson          gson = new Gson();

	/**
	 * Create the initial configuration.
	 */
	@BeforeAll
	static void setUp()
		throws Exception
	{
		trial = new Configuration()
			.setName("ConsecutiveUpdateTest")
			.setRows(1)
			.setCols(1)
			.setComment("Initial comment")
			.setDatasetType(DatasetType.TRIAL)
			.setTraits(Arrays.asList(
				new Trait().setName("t1").setType("int").setmType("multi"),
				new Trait().setName("t2").setType("categorical").setmType("single")
			));

		trial.setData(new Cell[1][1]);
		Cell cell = new Cell();
		cell.setName("g1");
		cell.setValues(trial.getTraits().stream().map(t -> (String) null).collect(Collectors.toList()));
		cell.setDates(trial.getTraits().stream().map(t -> (String) null).collect(Collectors.toList()));
		trial.getData()[0][0] = cell;

		setUpClient();
	}

	/**
	 * Try sharing it initially.
	 */
	@Order(1)
	@Test
	void shareConfig()
		throws Exception
	{
		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		trial = result.data;
		Assertions.assertNotNull(trial.getUuid());
	}

	/**
	 * Remove the configuration from the database again.
	 */
	@AfterAll
	static void breakDown()
		throws Exception
	{
		WebTarget target = client.target(URL)
								 .path(trial.getUuid())
								 .queryParam("name", trial.getName());

		Response resp = target.request(MediaType.APPLICATION_JSON)
							  .delete();

		Assertions.assertEquals(200, resp.getStatus());
	}
}
