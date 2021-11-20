package com.gmall.product.utils;

import org.apache.commons.io.FilenameUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.util.ClassUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public class FastDFSUtils {
    private static TrackerClient trackerClient;
    private static final  String IMGURL = "http://101.200.144.97:8080/";
    // 初始化全局数据
    static {
        String file = ClassUtils.getDefaultClassLoader().getResource("fdfs_client.conf").getPath();
        try {
            ClientGlobal.init(file);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        trackerClient = new TrackerClient();
    }

    // 文件上传
    public static String fileUpload(MultipartFile file) {

        // 获取问价系统搞得Ip和端口号信息
        try {
            TrackerServer trackerServer = trackerClient.getConnection();
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            // 获取文件后缀
            String fileExt = FilenameUtils.getExtension(file.getOriginalFilename());
            // 开始上传文件
            String path = storageClient1.upload_file1(file.getBytes(), fileExt, null);
            return IMGURL + path;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteFile(String logUrl) {
        try {
            TrackerServer trackerServer = trackerClient.getConnection();
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            // 获取文件系统上的文件Id
            String fileId = logUrl.replace(IMGURL, "");
            storageClient1.delete_file1(fileId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
