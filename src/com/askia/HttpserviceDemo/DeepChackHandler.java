package com.askia.HttpserviceDemo;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class DeepChackHandler implements HttpHandler {

    public DeepChackHandler() {
    }

    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        System.out.println("处理新请求:" + requestMethod);
        if (requestMethod.equalsIgnoreCase("POST")) {
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/html;charset=utf-8");
            exchange.sendResponseHeaders(200, 0);
            String imageFormat = null; // 图片格式
            Headers requestHeaders = exchange.getRequestHeaders();
            Set<String> keySet = requestHeaders.keySet();
            Iterator<String> iter = keySet.iterator();
            InputStream requestBody = exchange.getRequestBody();
            OutputStream responseBody = exchange.getResponseBody();
            /*------------------------解析头--------------------------*/
            while (iter.hasNext()) {
                String key = iter.next();
                List<String> values = requestHeaders.get(key);
                if ("Format".equals(key)) {
                    imageFormat = values.get(0);
                }
            }
            System.out.println("收到上传文件格式：" + imageFormat);
            /*------------------------解析文件--------------------------*/
            // 获取字节输入流
            BufferedInputStream bis = new BufferedInputStream(requestBody);
            byte[] buffer = new byte[1024];
            int length = bis.read(buffer);
            readFile(bis, buffer, length);
            bis.close();
            responseBody.write(0);
            responseBody.flush();
            responseBody.close();
        }

    }

    private void readFile(BufferedInputStream bis, byte[] buffer, int length) {
        try {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            RequestFileItem fileInfo = getFileReadIndex(buffer);
            byte[] realFileData = null;
            if (fileInfo.startIndex >= length - 1) {
                length = bis.read(buffer);
                realFileData = cartByte(buffer, 0, length - 1);
            } else {
                realFileData = cartByte(buffer, fileInfo.startIndex, length - 1);
            }
            // 校验文件是否小于1024K
            if (new String(realFileData).contains(fileInfo.boundary)) {// 文件非常小(小于1024),第一个1024字节就包含了文件
                realFileData = cartByte(realFileData, 0, GetIndexOf(realFileData, (fileInfo.boundary).getBytes()) - 3);
                outputStream.write(realFileData);
                // 需要保存文件
                saveUploadImage(outputStream, fileInfo.fileName);
                return;
            }
            outputStream.write(realFileData);
            byte[] newBuffer = null;
            while ((length = bis.read(buffer)) != -1) {
                if (!new String(buffer).contains(fileInfo.boundary)) {
                    outputStream.write(buffer, 0, length);
                } else {
                    // 结束符前有个换行符 -3
                    int endIndex = GetIndexOf(buffer, (fileInfo.boundary).getBytes()) - 3;
                    byte[] realFieData = cartByte(buffer, 0, endIndex);
                    outputStream.write(realFieData);
                    // 需要保存文件
                    saveUploadImage(outputStream, fileInfo.fileName);
                    // 检测是否还有文件接收
                    if (length != -1) {
                        byte[] nextFileData = cartByte(buffer, endIndex + 3, length - 1);
                        length = bis.read(buffer);
                        if (length != -1) {
                            nextFileData = byteMerger(nextFileData, cartByte(buffer, 0, length - 1));
                            newBuffer = nextFileData;
                            break;
                        }
                    }
                }
            }
            if (newBuffer != null && newBuffer.length > 0) {
                readFile(bis, newBuffer, newBuffer.length);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private RequestFileItem getFileReadIndex(byte[] buffer) {
        // 读四个\n符号就检查是不是文件
        try {
            for (int i = 0; i < buffer.length; i++) {
                int startIndex = getLineIndex(buffer, 4 * i);
                int endIndex = getLineIndex(buffer, 4 * (i + 1));
                byte[] fourLines = cartByte(buffer, startIndex, endIndex); // 该字节数组包含文件信息
                String fileInfo = new String(fourLines);
                if (fileInfo.contains("filename")) {
                    RequestFileItem info = new RequestFileItem();
                    // 数据结尾
                    info.boundary = fileInfo.substring(0,
                            fileInfo.indexOf("\n") - 1);
                    fileInfo = fileInfo.substring(fileInfo.indexOf("\n") + 1,
                            fileInfo.length());
                    // 文件名称
                    info.fileName = fileInfo.substring(
                            fileInfo.indexOf("filename=\"")
                                    + "filename=\"".length(),
                            fileInfo.indexOf("\n") - "\"\n".length());
                    // 编码类型
                    info.contentType = fileInfo.substring(
                            fileInfo.indexOf("Content-Type:"),
                            fileInfo.length());
                    // 文件开始
                    info.startIndex = endIndex + 2;
                    return info;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveUploadImage(ByteArrayOutputStream outputStream,
                                 String fileName) throws IOException {
        // outputStream.toByteArray() 返回Byte[]
        File saveWay = new File("E://upload/");
        if (!saveWay.exists())
            saveWay.mkdirs();
        File uploadFile = new File(saveWay.getAbsolutePath() + "/" + fileName);
        if (uploadFile.exists()) {
            uploadFile.delete();
        }
        uploadFile.createNewFile();
        OutputStream os = new FileOutputStream(uploadFile);
        os.write(outputStream.toByteArray());
        os.close();
        System.out.println(">>----------------------文件:" + fileName + "保存成功");
    }

    public int getLineIndex(byte[] source, int lineNumber) {
        if (lineNumber <= 0)
            return 0;
        int lineCount = 0;
        for (int k = 0; k < source.length; k++) {
            if (lineCount == lineNumber)
                return k;
            if (source[k] == "\n".getBytes()[0] && lineCount <= lineNumber)
                lineCount++;
        }
        return 0;
    }

    public byte[] cartByte(byte[] source, int beginIndex, int endIndex) {
        if (source == null || source.length <= 0 || endIndex - beginIndex <= 0)
            return null;
        int byteLength = (endIndex + 1) - beginIndex;
        byte[] temp = new byte[byteLength];
        for (int i = 0; i < byteLength; i++) {
            temp[i] = source[i + beginIndex];
        }
        return temp;
    }

    class RequestFileItem {
        public String fileName;
        public String boundary;
        public String contentType;
        public int startIndex;
    }

    private int GetIndexOf(byte[] source, byte[] part) {
        if (source == null || part == null || source.length == 0
                || part.length == 0)
            return -1;

        int i, j;
        for (i = 0; i < source.length; i++) {
            if (source[i] == part[0]) {
                for (j = 1; j < part.length; j++) {
                    if (source[i + j] != part[j])
                        break;
                }
                if (j == part.length)
                    return i;
            }
        }
        return -1;
    }

    private byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

}
