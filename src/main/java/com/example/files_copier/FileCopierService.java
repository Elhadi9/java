package com.example.files_copier;


import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
@EnableScheduling
class FileCopierService {


    @Value("${source.dir}")
    private String SOURCE_DIR;
    @Value("${dest.dir}")
    private String DEST_DIR;
    @Value("${dest.host}")
    private String DEST_HOST;
    @Value("${dest.username}")
    private String DEST_USERNAME;
    @Value("${dest.password}")
    private String DEST_PASSWORD;
    @Value("${redis.host}")
    private String redisHost;
    @Value("${redis.port}")
    private int redisPort;
    private final Set<String> copiedFiles = new HashSet<>();
    private static int failure = 0;
    private static int success = 0;
    private final FileCopierStats stats = new FileCopierStats();


    @Autowired
    @Lazy
    private RedisTemplate<String, FileCopierStats> redisTemplate;

    private static final String KEY = "file_copier_stats";


    public void addOrUpdateData(FileCopierStats record) {
        redisTemplate.opsForValue().set(KEY, record);
    }

    public FileCopierStats getData() {
        return redisTemplate.opsForValue().get(KEY);
    }


    @Bean
    public RedisTemplate<String, FileCopierStats> redisTemplate() {
        RedisTemplate<String, FileCopierStats> template = new RedisTemplate<>();
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        template.setConnectionFactory(connectionFactory);
        // Configure Jackson JSON serializer
        Jackson2JsonRedisSerializer<FileCopierStats> serializer = new Jackson2JsonRedisSerializer<>(FileCopierStats.class);
        template.setDefaultSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    @Scheduled(fixedRateString = "${scheduling.time}") // Runs every 60 seconds
    public void copyFiles() {
        System.err.println("Periodic time :: " + new Date().toString());
        File sourceFolder = new File(SOURCE_DIR);
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            System.err.println("Source directory does not exist: " + SOURCE_DIR);
            return;
        }
        File[] files = sourceFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().contains("close") && !copiedFiles.contains(file.getName())) {
                    stats.setCopyTime(new Date().toString());
                    try {
                        copyFileToRemote(file);
                        copiedFiles.add(file.getName());
                        success++;
                        stats.setSuccess(success);
                    } catch (IOException e) {
                        System.err.println("Failed to copy: " + file.getName());
                        failure++;
                        stats.setFail(failure);
                        e.printStackTrace();
                    } finally {
                        addOrUpdateData(stats);
                    }
                }
            }
        }
    }

    private void copyFileToRemote(File file) throws IOException {
        try (SSHClient sshClient = setupSshj(); SFTPClient sftpClient = sshClient.newSFTPClient()) {
            try {
                sftpClient.stat(DEST_DIR);
            } catch (Exception e) {
                sftpClient.mkdirs(DEST_DIR);
                System.out.println("Created destination directory: " + DEST_DIR);
            }
            String destinationPath = DEST_DIR + "/" + file.getName();
            sftpClient.put(SOURCE_DIR + "/" + file.getName(), destinationPath);
            System.out.println("File copied to: " + destinationPath);
        }
    }


    private SSHClient setupSshj() throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(DEST_HOST);
        client.useCompression();
        client.authPassword(DEST_USERNAME, DEST_PASSWORD);
        return client;
    }
}