package com.javamaster44.logic;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SaveManager {
    private static final String SAVE_FILE = "user.dat";
    private static final String KEY = "ThisIsASecretKey"; // 16 chars for AES-128
    private static final String ALGORITHM = "AES";

    public static void save(Map<String, Integer> inventory, int money) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("MONEY=").append(money).append("\n");
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            String encrypted = encrypt(sb.toString());
            Files.writeString(new File(SAVE_FILE).toPath(), encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class SaveData {
        public int money = 0;
        public Map<String, Integer> inventory = new HashMap<>();
    }

    public static SaveData load() {
        SaveData data = new SaveData();
        File file = new File(SAVE_FILE);
        if (!file.exists()) return data;

        try {
            String encrypted = Files.readString(file.toPath());
            String decrypted = decrypt(encrypted);
            String[] lines = decrypted.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("=");
                if (parts[0].equals("MONEY")) {
                    data.money = Integer.parseInt(parts[1]);
                } else {
                    data.inventory.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        } catch (Exception e) {
            System.out.println("Save file corrupted or invalid.");
        }
        return data;
    }

    private static String encrypt(String value) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encryptedValue = cipher.doFinal(value.getBytes());
        return Base64.getEncoder().encodeToString(encryptedValue);
    }

    private static String decrypt(String encrypted) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decodedValue = Base64.getDecoder().decode(encrypted);
        byte[] decryptedVal = cipher.doFinal(decodedValue);
        return new String(decryptedVal);
    }
}