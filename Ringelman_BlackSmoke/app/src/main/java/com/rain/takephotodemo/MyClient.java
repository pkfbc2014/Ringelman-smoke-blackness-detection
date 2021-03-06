package com.rain.takephotodemo;

import com.dtflys.forest.annotation.DataFile;
import com.dtflys.forest.annotation.JSONBody;
import com.dtflys.forest.annotation.Query;
import com.dtflys.forest.annotation.Request;
import com.dtflys.forest.annotation.Post;
import com.dtflys.forest.callback.OnProgress;
import com.dtflys.forest.extensions.DownloadFile;

import java.io.File;
import java.util.Map;

public interface MyClient {
    @Request(
            //url = "http://192.168.229.162:4000",
            //url = "http://10.151.202.101:4000/hello?", // 校园网
            url = "http://47.95.148.117:8090",
            headers = "Accept: text/plain"
    )
    String sendRequest(@Query("myName") String username);

    @Post(url = "http://47.95.148.117:8090/upload",timeout = 120000)
    String upload(@DataFile("file") String filePath, OnProgress onProgress);

    @Post(url = "http://47.95.148.117:8090/download",timeout = 120000)
    @DownloadFile(dir = "${0}", filename = "${1}")
    File downloadFile(String dir, String filename, OnProgress onProgress, @JSONBody("downloadPath") String path);
}
