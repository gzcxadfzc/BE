package com.pkg.s3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record PreAssignedUrl(
        String originUrl,
        String destinationKey
) {
    public static PreAssignedUrl toBookPrefix(String originUrl) {
        return new PreAssignedUrl(originUrl, S3KeyGen.createBookKey(S3KeyGen.Ext.PNG));
    }

    public static Map<String, PreAssignedUrl> mapOfBookPrefix(List<String> urls) {
        Map<String, PreAssignedUrl> map = new HashMap<>();
        for(String url : urls) {
            map.put(url, toBookPrefix(url));
        }
        return map;
    }
}
