package com.feraijo.excel;

import com.feraijo.auth.Authorizer;
import com.feraijo.main.ExcelCell;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;



/**
 * Created by Feraijo on 05.12.2016.
 */
public class UrlParser {

    //get params (ip, lg, to) from vk responce
    public String[] getOAuthParams(String html){
        Document doc = Jsoup.parse(html);
        String[] params_arr = new String[3];

        Elements ip = doc.select("input[name=ip_h]");
        params_arr[0] = ip.attr("value");

        Elements lg = doc.select("input[name=lg_h]");
        params_arr[1] = lg.attr("value");

        Elements to = doc.select("input[name=to]");
        params_arr[2] = to.attr("value");
        return params_arr;
    }

    //get description from lumna responce
    private String getDesc(String html){
        Document doc = Jsoup.parse(html);
        Elements desc_el = doc.select("p[style=text-indent:20px; text-align:justify]");
        return desc_el.text();
    }

    //get photo and description from ozon responce
    private String[] getPhotoAndDesc(String html) {
        Document doc = Jsoup.parse(html);
        String[] result = new String[2];

        Elements img_el = doc.select("img[class=eMicroGallery_fullImage]");
        String img_src_bad = img_el.attr("src");
        result[0] = "http:".concat(img_src_bad);

        Elements desc_el = doc.select("div[class=eProductDescriptionText_text]");
        result[1] = desc_el.text();

        return result;
    }
}
