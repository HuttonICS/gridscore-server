import com.google.gson.Gson;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.*;
import jhi.gridscore.server.pojo.Configuration;
import jhi.gridscore.server.pojo.*;
import org.junit.jupiter.api.*;

import java.util.Arrays;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrialMultiSharingTest extends ConfigTest
{
	private static Configuration trial;
	private        Gson          gson = new Gson();

	/**
	 * Create the initial configuration.
	 */
	@BeforeAll
	static void setUp()
		throws Exception
	{
		trial = new Configuration();
		trial.setName("Trial-test-multi");
		trial.setRows(3);
		trial.setCols(1);
		trial.setTraits(Arrays.asList(
			new Trait().setName("t1").setType("categorical").setmType("multi").setRestrictions(new Restrictions().setCategories(Arrays.asList("yellow", "white"))),
			new Trait().setName("t2").setType("date").setmType("multi")
		));
		Cell[][] data = new Cell[3][1];
		data[0][0] = new Cell()
			.setValues(Arrays.asList(null, null))
			.setDates(Arrays.asList(null, null))
			.setName("g1");
		data[1][0] = new Cell()
			.setValues(Arrays.asList(null, null))
			.setDates(Arrays.asList(null, null))
			.setName("g2");
		data[2][0] = new Cell()
			.setValues(Arrays.asList(null, null))
			.setDates(Arrays.asList(null, null))
			.setName("g3");
		trial.setData(data);
		trial.setDatasetType(DatasetType.TRIAL);

		setUpClient();
	}

	/**
	 * Try sharing it initially.
	 */
	@Order(1)
	@Test
	void shareInitialConfig()
		throws Exception
	{
		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		trial = result.data;
		Assertions.assertNotNull(trial.getUuid());
	}

	@Test
	@Order(2)
	void shareActualData()
	{
		// Add some data
		Cell cell = trial.getData()[0][0];
		cell.getValues().set(0, gson.toJson(new String[]{"yellow", "white"}));
		cell.getDates().set(0, gson.toJson(new String[]{"2022-01-03", "2022-01-02"}));
		cell.getValues().set(1, gson.toJson(new String[]{"2022-03-03", "2022-03-04"}));
		cell.getDates().set(1, gson.toJson(new String[]{"2022-03-03", "2022-03-04"}));

		// Send it, then check the result
		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		Assertions.assertEquals(gson.toJson(new String[]{"white", "yellow"}), result.data.getData()[0][0].getValues().get(0));
		Assertions.assertEquals(gson.toJson(new String[]{"2022-01-02", "2022-01-03"}), result.data.getData()[0][0].getDates().get(0));

		// Store
		trial = result.data;

		// Now update some data
		cell = trial.getData()[0][0];
		cell.getValues().set(0, gson.toJson(new String[]{"yellow"}));
		cell.getDates().set(0, gson.toJson(new String[]{"2022-01-01"}));
		cell.setComment("This is a changed comment!");
		cell.setIsMarked(true);
		cell.setGeolocation(new Geolocation().setLat(1d).setLng(2d).setElv(3d));

		// Send it and expect the server to have joined them uniquely -> 3 multi-trait values
		result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		Assertions.assertEquals(gson.toJson(new String[]{"yellow", "white", "yellow"}), result.data.getData()[0][0].getValues().get(0));
		Assertions.assertEquals(gson.toJson(new String[]{"2022-01-01", "2022-01-02", "2022-01-03"}), result.data.getData()[0][0].getDates().get(0));
		Assertions.assertEquals("This is a changed comment!", result.data.getData()[0][0].getComment());
		Assertions.assertEquals(true, result.data.getData()[0][0].getIsMarked());
		Assertions.assertEquals(new Geolocation().setLat(1d).setLng(2d).setElv(3d), result.data.getData()[0][0].getGeolocation());

		trial = result.data;
	}

	/**
	 * Test adding traits. Add one trait first, then send off, then remove that trait and add a different one, then expect 4 traits back.
	 */
	@Order(3)
	@Test
	void testAddTrait()
	{
		// Add a trait to the config
		trial.getTraits().add(new Trait().setName("t3").setType("int").setRestrictions(new Restrictions().setMin(0d).setMax(10d)));
		// Extend data and dates lists
		Arrays.stream(trial.getData())
			  .forEach(r -> {
				  Arrays.stream(r)
						.forEach(c -> {
							c.getValues().add(null);
							c.getDates().add(null);
						});
			  });
		// Set values for germplasm 3
		trial.getData()[2][0].getDates().set(2, "2022-02-02");
		trial.getData()[2][0].getValues().set(2, "7");

		// Send
		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(trial, result.data);
		Assertions.assertEquals(gson.toJson(new String[]{"yellow", "white", "yellow"}), result.data.getData()[0][0].getValues().get(0));
		Assertions.assertEquals(gson.toJson(new String[]{"2022-01-01", "2022-01-02", "2022-01-03"}), result.data.getData()[0][0].getDates().get(0));

		// Now remove the trait
		trial.getTraits().remove(trial.getTraits().size() - 1);
		// Add a different one, now we still have 3 traits, just the third is different
		trial.getTraits().add(new Trait().setName("t4").setType("float").setRestrictions(new Restrictions().setMin(1d).setMax(3d)));
		// Update the values
		trial.getData()[2][0].getDates().set(2, "2022-02-01");
		trial.getData()[2][0].getValues().set(2, "3.5");

		// Send
		result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		Configuration res = result.data;
		// The result should have 4 traits
		Assertions.assertEquals(4, res.getTraits().size());
		Assertions.assertEquals(4, res.getData()[0][0].getValues().size());
		Assertions.assertEquals(gson.toJson(new String[]{"yellow", "white", "yellow"}), result.data.getData()[0][0].getValues().get(0));
		Assertions.assertEquals(gson.toJson(new String[]{"2022-01-01", "2022-01-02", "2022-01-03"}), result.data.getData()[0][0].getDates().get(0));
		// Check the values for trait 3 and 4 are correct for germplasm 3
		Assertions.assertEquals("7", res.getData()[2][0].getValues().get(2));
		Assertions.assertEquals("2022-02-02", res.getData()[2][0].getDates().get(2));
		Assertions.assertEquals("3.5", res.getData()[2][0].getValues().get(3));
		Assertions.assertEquals("2022-02-01", res.getData()[2][0].getDates().get(3));

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
