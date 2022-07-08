import com.google.gson.Gson;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.*;
import jhi.gridscore.server.pojo.Configuration;
import jhi.gridscore.server.pojo.*;
import org.junit.jupiter.api.*;

import java.util.*;
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
			.setCols(2)
			.setComment("Initial comment")
			.setDatasetType(DatasetType.TRIAL)
			.setTraits(Arrays.asList(
				new Trait().setName("t1").setType("int").setmType("multi"),
				new Trait().setName("t2").setType("categorical").setmType("single").setRestrictions(new Restrictions().setCategories(Arrays.asList("a", "b", "c")))
			));

		trial.setData(new Cell[2][1]);
		Cell cell = new Cell();
		cell.setName("g1");
		cell.setValues(trial.getTraits().stream().map(t -> (String) null).collect(Collectors.toList()));
		cell.setDates(trial.getTraits().stream().map(t -> (String) null).collect(Collectors.toList()));
		trial.getData()[0][0] = cell;
		cell = new Cell();
		cell.setName("g2");
		cell.setValues(trial.getTraits().stream().map(t -> (String) null).collect(Collectors.toList()));
		cell.setDates(trial.getTraits().stream().map(t -> (String) null).collect(Collectors.toList()));
		trial.getData()[1][0] = cell;

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

	@Order(2)
	@RepeatedTest(2)
	void sendUpdate()
		throws Exception
	{
		Cell cell = trial.getData()[0][0];
		cell.getValues().set(0, gson.toJson(new String[]{"3"}));
		cell.getDates().set(0, gson.toJson(new String[]{"2022-01-01"}));
		cell.getValues().set(1, "a");
		cell.getDates().set(1, "2022-01-02");
		cell.setComment(UUID.randomUUID().toString());
		trial.setComment(UUID.randomUUID().toString());
		trial.setCornerPoints(new Double[][]{new Double[]{1d, 2d}, new Double[]{3d, 4d}});
		trial.setMarkers(new Markers().setCorner("topleft").setEveryCol(3d).setEveryRow(4d));

		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(trial, result.data);
		trial = result.data;
	}

	@Order(3)
	@Test
	void addTrait()
		throws Exception
	{
		trial.getTraits().add(new Trait().setName("t3").setType("date").setmType("multi"));
		trial.setComment(UUID.randomUUID().toString());
		Cell cell = trial.getData()[0][0];
		cell.getValues().add(gson.toJson(new String[]{"2022-04-04", "2022-04-05", "2022-04-06"}));
		cell.getDates().add(gson.toJson(new String[]{"2022-04-05", "2022-04-06", "2022-04-07"}));
		cell.setComment(UUID.randomUUID().toString());
		cell = trial.getData()[1][0];
		cell.getValues().add(null);
		cell.getDates().add(null);
		cell.getValues().set(0, gson.toJson(new String[]{"3"}));
		cell.getDates().set(0, gson.toJson(new String[]{"2022-01-01"}));
		cell.getValues().set(1, "a");
		cell.getDates().set(1, "2022-01-02");
		cell.setComment(UUID.randomUUID().toString());

		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(trial, result.data);
		trial = result.data;
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
