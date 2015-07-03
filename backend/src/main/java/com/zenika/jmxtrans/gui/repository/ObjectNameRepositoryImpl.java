package com.zenika.jmxtrans.gui.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.jmxtrans.gui.AppConfig;
import com.zenika.jmxtrans.gui.model.ObjectNameRepresentation;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;

@Repository
public class ObjectNameRepositoryImpl implements ObjectNameRepository {

    private static final Logger logger = LoggerFactory.getLogger(ObjectNameRepositoryImpl.class);

    private Client client;
    private ObjectMapper mapper;

    @Autowired
    public void setClient(Client client) {
        this.client = client;
    }

    @Autowired
    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ObjectNameRepresentation objectName) throws JsonProcessingException {
        logger.info("Saving objectname");
        this.client.prepareIndex(AppConfig.INDEX, AppConfig.OBJECTNAME_TYPE)
                .setRefresh(true)
                .setSource(mapper.writeValueAsString(objectName))
                .execute().actionGet();
    }

    @Override
    public Collection<String> prefixNameSuggestion(String host, int port) {
        logger.info("Retrieving object names");
        SearchResponse response = this.client.prepareSearch(AppConfig.INDEX)
                .setTypes(AppConfig.OBJECTNAME_TYPE)
                .setQuery(
                        QueryBuilders.boolQuery()
                                .must(QueryBuilders.termQuery("host", host))
                                .must(QueryBuilders.termQuery("port", port)))
                .addAggregation(
                        AggregationBuilders.terms("names").field("name")
                                .order(Terms.Order.term(true)).size(0))
                .execute().actionGet();

        Collection<String> result = new ArrayList<>();

        Terms agg = response.getAggregations().get("names");
        for (Terms.Bucket bucket : agg.getBuckets()) {
            result.add(bucket.getKey());
        }

        return result;
    }

    @Override
    public Collection<String> prefixAttrSuggestion(String host, int port, String name) {
        logger.info("Retrieving attribute names");
        SearchResponse response = this.client.prepareSearch(AppConfig.INDEX)
                .setTypes(AppConfig.OBJECTNAME_TYPE)
                .addFields("attributes")
                .setQuery(
                        QueryBuilders.boolQuery()
                                .must(QueryBuilders.termQuery("host", host))
                                .must(QueryBuilders.termQuery("port", port))
                                .must(QueryBuilders.termQuery("name", name)))
                .execute().actionGet();

        Collection<String> result = new ArrayList<>();

        for (SearchHit hit : response.getHits().getHits()) {
            if (hit.field("attributes") != null
                    && hit.field("attributes").getValues() != null) {
                for (Object value : hit.field("attributes").getValues())
                    result.add(value.toString());
            }
        }

        return result;
    }

    @Override
    public void delete(String host, int port) {
        logger.info("Deleting object names");
        this.client.prepareDeleteByQuery(AppConfig.INDEX)
                .setTypes(AppConfig.OBJECTNAME_TYPE)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("host", host))
                        .must(QueryBuilders.termQuery("port", port)))
                .execute().actionGet();
    }
}
