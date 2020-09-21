package com.kakao.pay.test.sprinkling_money.rest;

import io.vertx.core.Vertx;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;

public class ValidationData {

    private Vertx vertx;
    private String schema;

    public ValidationData(Vertx vertx) {
        this.vertx=vertx;
        // base
        schema = vertx.fileSystem().readFileBlocking("config/validation/scheme").toString();
    }

    public HTTPRequestValidationHandler getValidation(String path) {

        String valid_scheme = vertx.fileSystem().readFileBlocking(path).toString();

        return HTTPRequestValidationHandler.create()
                        .addJsonBodySchema(schema)
                        .addJsonBodySchema(valid_scheme);
    }
}
