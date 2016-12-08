package com.feraijo.auth;

import com.feraijo.excel.UrlParser;
import com.feraijo.image.ImgResize;
import com.feraijo.main.ExcelCell;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;

/**
 * Класс, имитирующий авторизацию вконтакте.
 * Created by Feraijo on 16.11.2016.
 */



public class Authorizer {
    /*private static RequestConfig globalConfig = RequestConfig.custom()
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .build();*/
    private String login = "";
    private String password = "";
    private final String APP_ID = "";
    private String group_id = "";
    private String ip_h;
    private String lg_h;
    private String to;
    private static String token = "";
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0";



    public static void main(String[] args) {
        Authorizer inst = new Authorizer();

        try {
            try (CloseableHttpClient httpclient = HttpClients.createDefault()){

                inst.oauthOltu();

                String firstGetResponse = inst.getOAuth(httpclient);
                UrlParser urlp = new UrlParser();
                String[] params = urlp.getOAuthParams(firstGetResponse);
                inst.setIp_h(params[0]);
                inst.setLg_h(params[1]);
                inst.setTo(params[2]);
                String postLoginResponse = inst.getToken(httpclient);


                //inst.uploadGoodsFromExcel(inst, httpclient);


            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    private void oauthOltu(){
        try {
            OAuthClientRequest request = OAuthClientRequest
                    .tokenLocation("https://oauth.vk.com/authorize")
                    .setClientId(APP_ID)
                    .setClientSecret("sdf")
                    .setUsername(login)
                    .setPassword(password)
                    .setRedirectURI("https://oauth.vk.com/blank.html")
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setScope("market, photos")
                    .buildQueryMessage();
            String resp = request.getLocationUri();
            System.out.println(request);
            System.out.println(resp);
        } catch (OAuthSystemException e) {
            System.out.println(e.getMessage());
        }
    }

    private void uploadGoodsFromExcel(Authorizer inst, CloseableHttpClient httpclient) throws IOException {
        //сервер, на котороый нужно загружать фото
        String uplServerResp = inst.getUplServer(httpclient);
        String uplServer = inst.uploadUrlParse(uplServerResp);
        System.out.println("Сервер получен");
        ExcelCell exc = ExcelCell.getInstance();

        // массив ошибочных загрузок
        //int[] x = {}; //
        //for (int i : x) {
        for (int i = 1; i< exc.getMaxRows(); i++) {
            System.out.print(i+ ": ");
            try {

                String picUrl = exc.getCell(exc.PHOTO_C, i).getStringCellValue();
                //System.out.println("Картинка товара получена");

                //Загрузка фото на сервер, в ответ JSON объект, нужный для сохранения фото на сервере ВК
                String photoResponse = inst.uploadPhoto(httpclient, picUrl, uplServer);
                //System.out.println("Картинка загружена");
                String photo_id = inst.savePhotoOnVKServ(httpclient, photoResponse);
                //System.out.println("Картинка сохранена на сервере");

                //Добавление товара

                inst.addMarketGoods(httpclient, photo_id, exc, i);

                //System.out.println("Всё прошло успешно");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        exc.closeExcel();
    }

    //Метод для сохранения фотки на сервере вк
    private String uploadPhoto(CloseableHttpClient httpclient, String urlPhoto, String uplServer) throws IOException {

        HttpPost uploadFile = new HttpPost(uplServer);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        ImgResize imrs = new ImgResize();
        File f = imrs.getGoodPhoto(urlPhoto);

        f.deleteOnExit();
        builder.addBinaryBody(
                "file",
                new FileInputStream(f),
                ContentType.APPLICATION_OCTET_STREAM,
                f.getName()
        );

        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);
        try(CloseableHttpResponse response = httpclient.execute(uploadFile)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        }
    }

    private String uploadUrlParse(String resp){
        JSONObject obj = new JSONObject(resp);
        return obj.getJSONObject("response").getString("upload_url");
    }

    private void addMarketGoods (CloseableHttpClient httpclient, String input, ExcelCell exc, int row) throws IOException {
        JSONObject obj = new JSONObject(input);
        JSONArray arr = obj.getJSONArray("response");
        String name = "";

        URIBuilder builder;
        HttpGet request = null;
        try {
            builder = new URIBuilder("https://api.vk.com/method/market.add"); // add edit

            name = exc.getCell(exc.NAME_C,row).getStringCellValue();
            String description = exc.getCell(exc.DESCR_C, row).getStringCellValue();
            String price = Double.toString(exc.getCell(exc.PRICE_C, row).getNumericCellValue());
            String photo_id = arr.optJSONObject(0).get("pid").toString();

            builder.setParameter("owner_id", arr.optJSONObject(0).get("owner_id").toString()).
                    setParameter("name", name).
                    setParameter("description", description).
                    setParameter("category_id", "901").
                    setParameter("price", price).
                    setParameter("deleted", "0").
                    setParameter("main_photo_id", photo_id).
                    setParameter("access_token", token);
            request = new HttpGet(builder.build());
            request.setHeader("User-Agent", USER_AGENT);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            /*HttpEntity entity  = response.getEntity();
            String respStr = EntityUtils.toString(entity);*/
            if (response.getStatusLine().getStatusCode() != 200){
                throw new Exception("Всё очень плохо");
            }
            System.out.println("Товар " + name + " добавлен");
        } catch (Exception e){
            System.out.println(e.getMessage());
        }

    }

    public String getLumnaDesc(String req) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0";
            URIBuilder builder;
            HttpPost post = null;
            try {
                builder = new URIBuilder("http://www.lumna.ru/books");
                builder.setParameter("page", "shop.browse").
                        setParameter("view", "books").
                        setParameter("group", "div_book").
                        setParameter("keyword", req);
                post = new HttpPost(builder.build());
                post.setHeader("User-Agent", USER_AGENT);
            } catch (URISyntaxException e) {
                System.out.println(e.getMessage());
            }

            String location;
            try (CloseableHttpResponse response = httpclient.execute(post)) {
                location = response.getFirstHeader("Location").getValue();
            }

            HttpGet get = new HttpGet(location);
            get.setHeader("User-Agent", USER_AGENT);

            try (CloseableHttpResponse response = httpclient.execute(get)) {
                HttpEntity entity = response.getEntity();
                String respStr = EntityUtils.toString(entity);

                return respStr;
            }
        }
    }

    public String getOzonSearchRes(String req) throws IOException {

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0";
            URIBuilder builder;
            HttpGet request = null;
            try {
                builder = new URIBuilder("http://www.ozon.ru/");
                builder.setParameter("context", "search").
                        setParameter("text", req).
                        setParameter("group", "div_book").
                        setParameter("store", "1,0");
                request = new HttpGet(builder.build());
                request.setHeader("User-Agent", USER_AGENT);
            } catch (URISyntaxException e) {
                System.out.println(e.getMessage());
            }
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            }
        }

    }

    private String savePhotoOnVKServ(CloseableHttpClient httpclient, String input) throws IOException {
        JSONObject obj = new JSONObject(input);

        URIBuilder builder;
        HttpGet request = null;
        try {
            builder = new URIBuilder("https://api.vk.com/method/photos.saveMarketPhoto");
            builder.setParameter("group_id", group_id).
                    setParameter("photo", obj.opt("photo").toString()).
                    setParameter("server", obj.opt("server").toString()).
                    setParameter("hash", obj.opt("hash").toString()).
                    setParameter("crop_data", obj.opt("crop_data").toString()).
                    setParameter("crop_hash", obj.opt("crop_hash").toString()).
                    setParameter("access_token", token);
            request = new HttpGet(builder.build());
            request.setHeader("User-Agent", USER_AGENT);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            HttpEntity entity  = response.getEntity();
            return EntityUtils.toString(entity);

        }
    }

    private String getUplServer(CloseableHttpClient httpclient) throws IOException {

        URIBuilder builder;
        HttpGet request = null;
        try {
            builder = new URIBuilder("https://api.vk.com/method/photos.getMarketUploadServer");
            builder.setParameter("group_id", group_id).
                    setParameter("main_photo", "1").
                    setParameter("access_token", token);
            request = new HttpGet(builder.build());
            request.setHeader("User-Agent", USER_AGENT);
            /*request.setHeader("Cookie", "remixflash=18.0.0; remixscreen_depth=24; remixrefkey=87ee931febedb127af; " +
                    "audio_vol=9; remixseenads=1; remixab=1; remixvkcom_done=1; remixlang=0; " +
                    "remixdt=7200; remixtst=db9c3470; remixexp=1; remixlhk=0" +
                    "a5ca5246310344e6e");*/
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            HttpEntity entity  = response.getEntity();
            String respStr = EntityUtils.toString(entity);

            //urlPhoto = respStr.substring(respStr.indexOf("https"), respStr.lastIndexOf("\""));
            EntityUtils.consume(entity);
            return respStr;
        }

    }

    // Первый гет-запрос для получения данных для пост-запроса
    private String getOAuth(CloseableHttpClient httpclient) throws IOException {
        URIBuilder builder;
        HttpGet request = null;
        try {

            builder = new URIBuilder("https://oauth.vk.com/authorize");
            builder.setParameter("client_id", APP_ID).
                    setParameter("redirect_uri", "https://oauth.vk.com/blank.html").
                    setParameter("scope", "market,photos").
                    setParameter("response_type", "token").
                    setParameter("v", "5.60");
            request = new HttpGet(builder.build());
            request.setHeader("User-Agent", USER_AGENT);
            request.setHeader("Host","oauth.vk.com");
            request.setHeader("Cookie", "remixflash=18.0.0; remixscreen_depth=24; remixrefkey=30c213a320328e6239; audio_vol=9; remixseenads=0\n" +
                    "; remixab=1; remixvkcom_done=1; remixlang=0; remixdt=7200; remixtst=db9c3470; remixstid=1825436696_cba76ac13a2023d738\n" +
                    "; remixexp=1; remixlhk=c4f2a933bef2500b12");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            if (isValidStatus(200, response)) return null;
            HttpEntity entity  = response.getEntity();
            return EntityUtils.toString(entity);
        }
    }

    //Серия запросов с перенаправлением до страницы с токеном.
    private String getToken(CloseableHttpClient httpclient) throws IOException {
        URIBuilder builder;
        HttpPost post = null;
        try {
            builder = new URIBuilder("https://login.vk.com/?act=login&soft=1");
            builder.setParameter("_origin", "https://oauth.vk.com").
                    setParameter("email", login).
                    setParameter("pass", password).
                    setParameter("ip_h", ip_h).
                    setParameter("lg_h", lg_h).
                    setParameter("to", to).
                    setParameter("expire", "0");

            post = new HttpPost(builder.build());
            post.setHeader("User-Agent", USER_AGENT);
            post.setHeader("Host","login.vk.com");
            post.setHeader("Cookie", "remixflash=18.0.0; remixscreen_depth=24; remixrefkey=30c213a320328e6239; audio_vol=9; remixseenads=0\n" +
                    "; remixab=1; remixvkcom_done=1; remixlang=0; remixdt=7200; t=0d8befc1230374753703b3a4; remixtst=db9c3470\n" +
                    "; remixstid=1825436696_cba76ac13a2023d738; remixexp=1; remixlhk=c4f2a933bef2500b12");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String location;
        try (CloseableHttpResponse response = httpclient.execute(post)) {
            //if (isValidStatus(302, response)) return null;
            location = response.getFirstHeader("Location").getValue();
        }

        HttpGet get = new HttpGet(location);
        get.setHeader("User-Agent", USER_AGENT);
        /*post = new HttpPost(location);
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("Host","oauth.vk.com");
        post.setHeader("Cookie", "remixflash=18.0.0; remixscreen_depth=24; remixrefkey=87ee931febedb127af; " +
                "audio_vol=9; remixseenads=1; remixab=1; remixvkcom_done=1; remixlang=0; " +
                "remixdt=7200; remixtst=db9c3470; remixexp=1; remixlhk=0a5ca5246310344e6e" +
                "; remixq_187a9788283ac6eab8df103e2a12fa57=eee68dcba22be4d95a");*/
        try (CloseableHttpResponse response = httpclient.execute(get)) {
            //if (isValidStatus(302, response)) return null;
            location = response.getFirstHeader("Location").getValue();
        }
        get = new HttpGet(location);
        get.setHeader("User-Agent", USER_AGENT);
        /*post = new HttpPost(location);
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("Host","login.vk.com");
        post.setHeader("Cookie", "remixflash=18.0.0; remixscreen_depth=24; remixrefkey=87ee931febedb127af;" +
                " audio_vol=9; remixseenads=1; remixab=1; remixvkcom_done=1; remixlang=0; remixdt=7200;" +
                " t=0d8befc1230374753703b3a4; remixtst=db9c3470; remixexp=1; remixlhk=0a5ca5246310344e6e;" +
                " h=1; s=1; l=322068165; p=3afef4e67e8652564f09df0054e110c1eb84e2d2d4824c30a15d6;" +
                " remixsid=2ae22cca8cffb6ba5fc79b781fb4861e3782b6f0794f4dcf0077d; remixsslsid=1");*/

        try (CloseableHttpResponse response = httpclient.execute(post)) {
            //if (isValidStatus(302, response)) return null;
            location = response.getFirstHeader("Location").getValue();
        }


        return location.split("#")[1].split("&")[0].split("=")[1];
    }

    private boolean isValidStatus(int statusCode, CloseableHttpResponse response) {
        int code = response.getStatusLine().getStatusCode();
        if (code != statusCode) {
            //System.out.println("Status: " + response.getStatusLine());
            if (code == 200){
                System.out.println("Введённый логин и/или пароль не верны.");
            }
            if (code == 403){
                System.out.println("Ограниченный доступ.");
            }
            if (code == 404){
                System.out.println("Такой страницы не существует.");
            }
            if (code >= 500){
                System.out.println("Ошибка сервера, попробуйте позже.");
            }
            return true;
        }
        return false;
    }

    private void setLogin(String login) {
        this.login = login;
    }

    private void setPassword(String password) {
        this.password = password;
    }

    private void setIp_h(String ip_h) {
        this.ip_h = ip_h;
    }

    private void setLg_h(String lg_h) {
        this.lg_h = lg_h;
    }

    private void setTo(String to) {
        this.to = to;
    }
}
