package com.focess.api.util.network;

import com.google.common.collect.Maps;
import okhttp3.*;
import org.apache.poi.ss.formula.functions.T;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

public class NetworkHandler {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType TEXT = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    private static final OkHttpClient CLIENT;

    static {
        X509TrustManager[] managers = new X509TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null,managers,new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
        CLIENT = new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory(),managers[0]).hostnameVerifier((hostname, session)->true).build();
    }

    public HttpResponse request(String url,Map<String,Object> data,RequestType requestType) {
        return this.request(url,data,Maps.newHashMap(),TEXT,requestType);
    }

    public HttpResponse request(String url,RequestType requestType) {
        return this.request(url, Maps.newHashMap(),requestType);
    }

    public HttpResponse request(String url, Map<String,Object> data,Map<String,String> header,MediaType mediaType, RequestType requestType) {
        if (requestType == RequestType.GET)
            return get(url,data,header);
        else if (requestType == RequestType.POST)
            return post(url,data,header,mediaType);
        return HttpResponse.ofNull();
    }

    private String process(Map<String,Object> data) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String key : data.keySet())
            stringBuilder.append(key).append('=').append(data.get(key)).append('&');
        if (stringBuilder.length() != 0)
            return stringBuilder.substring(0,stringBuilder.length() - 1);
        return "";
    }

    public HttpResponse post(String url, Map<String, Object> data,Map<String,String> header,MediaType mediaType) {
        String value;
        if (mediaType.equals(JSON))
            value = new com.focess.api.util.json.JSON(data).toJson();
        else value = process(data);
        RequestBody requestBody = RequestBody.create(mediaType,value);
        Request request = new Request.Builder().url(url).headers(Headers.of(header)).post(requestBody).build();
        try {
            Response response = CLIENT.newCall(request).execute();
            return new HttpResponse(response.code(),response.headers(),response.body().string());
        } catch (IOException e) {
            return new HttpResponse(e);
        }
    }

    public HttpResponse get(String url, Map<String,Object> data,Map<String,String> header) {
        Request request;
        if (data.size() != 0)
            request = new Request.Builder().url(url + "?" + process(data)).get().headers(Headers.of(header)).build();
        else
            request = new Request.Builder().url(url).get().headers(Headers.of(header)).build();
        try {
            Response response = CLIENT.newCall(request).execute();
            return new HttpResponse(response.code(),response.headers(),response.body().string());
        } catch (IOException e) {
            return new HttpResponse(e);
        }
    }

    public enum RequestType {
        GET,POST;
    }

}
