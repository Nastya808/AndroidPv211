package itstep.learning.androidpv211.orm;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NbuRate {
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT );

    private int    r030;
    private String txt;
    private double rate;
    private String cc;
    private Date   exchangeDate;

    public static NbuRate fromJsonObject( JSONObject jsonObject ) throws JSONException {
        NbuRate res = new NbuRate();
        res.setR030( jsonObject.getInt( "r030" ) );
        res.setTxt( jsonObject.getString( "txt" ) );
        res.setRate( jsonObject.getDouble( "rate" ) );
        res.setCc( jsonObject.getString( "cc" ) );
        try {
            res.setExchangeDate(
                    dateFormat.parse(
                            jsonObject.getString( "exchangedate" ) ) );
        }
        catch( ParseException ex ) {
            throw new JSONException( ex.getMessage() );
        }
        return res;
    }

    public int getR030() {
        return r030;
    }

    public void setR030(int r030) {
        this.r030 = r030;
    }

    public String getTxt() {
        return txt;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public Date getExchangeDate() {
        return exchangeDate;
    }

    public void setExchangeDate(Date exchangeDate) {
        this.exchangeDate = exchangeDate;
    }
}
/*
ORM
{
    "r030": 12,
    "txt": "Алжирський динар",
    "rate": 0.30886,
    "cc": "DZD",
    "exchangedate": "03.04.2025"
}
 */