package com.redislabs.university.RU102J.dao;

import com.redislabs.university.RU102J.HostPort;
import com.redislabs.university.RU102J.TestKeyManager;
import com.redislabs.university.RU102J.api.MeterReading;
import com.redislabs.university.RU102J.api.SiteStats;
import org.junit.*;
import redis.clients.jedis.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.ZonedDateTime;

public class SiteStatsDaoRedisImplTest {
    private static JedisPool jedisPool;
    private static Jedis jedis;
    private static TestKeyManager keyManager;

    @BeforeClass
    public static void setUp() throws Exception {
        String password = HostPort.getRedisPassword();

        if (password.length() > 0) {
            jedisPool = new JedisPool(new JedisPoolConfig(), HostPort.getRedisHost(), HostPort.getRedisPort(), 2000, password);
        } else {
            jedisPool = new JedisPool(HostPort.getRedisHost(), HostPort.getRedisPort());
        }

        jedis = new Jedis(HostPort.getRedisHost(), HostPort.getRedisPort());

        if (password.length() > 0) {
            jedis.auth(password);
        }

        keyManager = new TestKeyManager("test");
    }

    @AfterClass
    public static void tearDown() {
        jedisPool.destroy();
        jedis.close();
    }

    @After
    public void flush() {
        keyManager.deleteKeys(jedis);
    }

    @Test
    @Ignore
    public void homework23() {
        jedis.set("a", "foo");
        jedis.set("b", "bar");
        jedis.set("c", "baz");

        Transaction t = jedis.multi();

        Response<String> r1 = t.set("b", "1");
        Response<Long> r2 = t.incr("a"); // <- increment a String? JedisDataException...
        Response<String> r3 = t.set("c", "100");

        t.exec(); // <- ... but it's thrown just here

        r1.get();
        r2.get();
        r3.get();
    }

    @Test
    @Ignore
    public void homework24() {
        Pipeline pipeline = jedis.pipelined();

        Response<Long> length = pipeline.zcard("set");
        if (length.get() < 1000) { // <- this fails, because result is not yet available. Call sync() first.

        }

        pipeline.sync();
    }

    @Test
    public void findById() {
        MeterReading r1 = generateMeterReading(1);
        SiteStatsDao dao = new SiteStatsDaoRedisImpl(jedisPool);
        dao.update(r1);
        SiteStats stats = dao.findById(1);
        assertThat(stats.getMeterReadingCount(), is(1L));
        assertThat(stats.getMinWhGenerated(), is(r1.getWhGenerated()));
        assertThat(stats.getMaxWhGenerated(), is(r1.getWhGenerated()));
    }

    // Challenge #3
    @Test
    public void testUpdate() {
        SiteStatsDao dao = new SiteStatsDaoRedisImpl(jedisPool);
        MeterReading r1 = generateMeterReading(1);
        r1.setWhGenerated(1.0);
        r1.setWhUsed(0.0);
        MeterReading r2 = generateMeterReading(1);
        r2.setWhGenerated(2.0);
        r2.setWhUsed(0.0);

        dao.update(r1);
        dao.update(r2);
        SiteStats stats = dao.findById(1L, r1.getDateTime());
        assertThat(stats.getMaxWhGenerated(), is(2.0));
        assertThat(stats.getMinWhGenerated(), is(1.0));
        assertThat(stats.getMaxCapacity(), is(2.0));
    }

    private MeterReading generateMeterReading(long siteId) {
        MeterReading reading = new MeterReading();
        reading.setSiteId(siteId);
        reading.setDateTime(ZonedDateTime.now());
        reading.setTempC(15.0);
        reading.setWhGenerated(0.025);
        reading.setWhUsed(0.015);
        return reading;
    }
}