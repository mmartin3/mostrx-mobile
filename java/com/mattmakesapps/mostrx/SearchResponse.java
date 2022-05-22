package com.mattmakesapps.mostrx;

import java.io.Serializable;
import java.util.List;

public class SearchResponse implements Serializable {
    public List<SearchResult> results;

    public static class SearchResult implements Serializable {
        public String name;
        public String slug;
    }
}
