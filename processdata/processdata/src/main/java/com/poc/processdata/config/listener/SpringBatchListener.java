package com.poc.processdata.config.listener;

import com.poc.processdata.AzureADLSPush;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SpringBatchListener implements JobExecutionListener {

    @Value("${spring.batch.file.result}")
    private String resultPath;

    @Value("${spring.batch.file.filePath}")
    private String filePath;

    @Value("${spring.batch.file.decryptedFilePath}")
    private String decryptedFilePath;

    @Autowired
    private AzureADLSPush azureADLSPush;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    private static final String SECRET_KEY = "MySecretKey12345";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        decrypt();
    }

    public void decrypt() {
        try {
            SecretKey secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            File inputFile = new File(filePath);
            File outputFile = new File(decryptedFilePath);
            try (InputStream inputStream = new FileInputStream(inputFile);
                 OutputStream outputStream = new FileOutputStream(outputFile);
                 CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            ) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    cipherOutputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            System.err.println("Error encrypting/decrypting file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void afterJob(JobExecution jobExecution) {
        try {
// Calculate checksum, record count, and file name
            String checksum = calculateMD5Checksum(filePath);
            int recordCount = getRecordCount(filePath);
            String fileName = getFileName(filePath);
            String qcFileName = fileName.substring(0, fileName.indexOf(".")) + "-qc.txt";
// Write to QC file
            writeQCFile(qcFileName, fileName, recordCount, checksum);
            azureADLSPush.pushToADLS();
            System.out.println("QC file generated: " + qcFileName);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String calculateMD5Checksum(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = new FileInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static int getRecordCount(String filePath) throws IOException {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while (reader.readLine() != null) {
                count++;
            }
        }
        return count - 1;
    }

    public String getFileName(String filePath) {
        File file = new File(filePath);
        return file.getName();
    }

    public void writeQCFile(String qcFileName, String fileName, int recordCount, String checksum) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultPath + "\\" + qcFileName))) {
            writer.write(fileName + "|" + recordCount + "|" + checksum);
        }
    }

}
