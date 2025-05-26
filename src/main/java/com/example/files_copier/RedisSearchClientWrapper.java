package com.example.files_copier;
import redis.clients.jedis.Jedis;
import io.redisearch.client.Client;


public class RedisSearchClientWrapper {
    private final String host;
    private final int port;
    private final String indexName;
    private final int timeout;
    private Jedis jedis;
    private Client redisSearchClient;

    public RedisSearchClientWrapper(String host, int port, int timeout, String indexName) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.indexName = indexName;
        createJedisConnection(indexName);
    }

    private void createJedisConnection(String indexName) {
        try {
            if (jedis != null) {
                jedis.close();// Close the previous connection if any
            jedis = null;
            }
            jedis = new Jedis(host, port, timeout);
            jedis.connect();
            redisSearchClient = new Client(indexName, jedis);
            System.out.println("Connected to Redis successfully.");
        } catch (Exception e) {
            System.err.println("Failed to connect to Redis: " + e.getMessage());
        }
    }

    public Client fetchClient() {
        try {
            reconnectIfNeeded();
        }
        catch (Exception e) {
            System.err.println("Failed to connect to Redis: " + e.getMessage());
        }
        return redisSearchClient;
    }

    public void reconnectIfNeeded() {
        try {
                System.out.println("Connection lost. Reconnecting...");
                createJedisConnection(indexName);

        } catch (Exception e) {
            System.err.println("Error during reconnect: " + e.getMessage());
        }
    }

    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}