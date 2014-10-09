package org.wordpress.android.ui.stats2.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;

public class VisitsModel extends RealmObject {
    private String fields; // Holds a JSON Object
    private String data; // Holds a JSON Object
    private String unit;

    @Index
    private String date;

    @Index
    private String blogID;

    @Ignore
    private JSONArray dataJSON; // dummy variable
    @Ignore
    private JSONArray fieldsJSON;// dummy variable

    public String getBlogID() {
        return blogID;
    }

    public void setBlogID(String blogID) {
        this.blogID = blogID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public JSONArray getDataJSON() {
        JSONArray jArray = null;
        try {
            String categories = StringUtils.unescapeHTML(this.getData() != null ? this.getData() : "[]");
            if (TextUtils.isEmpty(categories)) {
                jArray = new JSONArray();
            } else {
                jArray = new JSONArray(categories);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, this.getClass().getName() + " cannot convert the string to JSON", e);
            return null;
        }
        return jArray;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public JSONArray getFieldsJSON() {
        JSONArray jArray = null;
        try {
            String categories = StringUtils.unescapeHTML(this.getFields() != null ? this.getFields() : "[]");
            if (TextUtils.isEmpty(categories)) {
                jArray = new JSONArray();
            } else {
                jArray = new JSONArray(categories);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, this.getClass().getName() + " cannot convert the string to JSON", e);
            return null;
        }
        return jArray;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }

    public String getFields() {
        return fields;
    }
}
