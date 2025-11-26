package com.yinlian.test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class DeviceScanner {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        String subnet = "192.168.1";
        int port = 8091;
        int timeout = 200; // ms

        System.out.println("Scanning " + subnet + ".1-255 on port " + port + "...");

        ExecutorService es = Executors.newFixedThreadPool(50);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 1; i < 255; i++) {
            String ip = subnet + "." + i;
            futures.add(es.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    return ip;
                } catch (Exception e) {
                    return null;
                }
            }));
        }

        es.shutdown();

        List<String> foundDevices = new ArrayList<>();
        for (Future<String> f : futures) {
            String ip = f.get();
            if (ip != null) {
                System.out.println("Found device at: " + ip);
                foundDevices.add(ip);
            }
        }

        if (foundDevices.isEmpty()) {
            System.out.println("No devices found.");
        } else {
            System.out.println("Scan complete. Found " + foundDevices.size() + " devices.");
        }
    }
}
