package com.national.utility.billing.config;

import com.national.utility.billing.service.LocationService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class LocationOpenApiCustomizer {

    private final LocationService locationService;

    @Bean
    public OpenApiCustomizer locationDropdownCustomizer() {
        return this::applyLocationDropdowns;
    }

    private void applyLocationDropdowns(OpenAPI openApi) {
        if (!locationService.isLoaded() || openApi.getComponents() == null
                || openApi.getComponents().getSchemas() == null) {
            return;
        }

        Map<String, Schema> schemas = openApi.getComponents().getSchemas();

        applyEnumsToLocationSchema(schemas.get("LocationSelection"));
        applyEnumsToLocationSchema(schemas.get("LocationSelectionDto"));

        applyLocationPropertyEnums(schemas.get("CustomerRequest"), "location");
        applyLocationPropertyEnums(schemas.get("InviteCustomerRequest"), "location");
    }

    @SuppressWarnings("rawtypes")
    private void applyLocationPropertyEnums(Schema parentSchema, String propertyName) {
        if (parentSchema == null || parentSchema.getProperties() == null) {
            return;
        }
        Object locationProperty = parentSchema.getProperties().get(propertyName);
        if (locationProperty instanceof Schema locationSchema) {
            applyEnumsToLocationSchema(locationSchema);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applyEnumsToLocationSchema(Schema schema) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }

        setEnum(schema, "province", locationService.getProvinces());
        setEnum(schema, "district", locationService.getAllDistricts());
        setEnum(schema, "sector", locationService.getAllSectors());
        setEnum(schema, "cell", locationService.getAllCells());

        List<String> villages = locationService.getAllVillages();
        if (villages.size() <= locationService.getSwaggerVillageEnumLimit()) {
            setEnum(schema, "village", villages);
        } else {
            Schema villageSchema = (Schema) schema.getProperties().get("village");
            if (villageSchema != null) {
                villageSchema.setDescription(
                        "Type village name (too many options for dropdown — use GET /api/locations/search?keyword=... if needed)");
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setEnum(Schema parent, String field, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Schema fieldSchema = (Schema) parent.getProperties().get(field);
        if (fieldSchema != null) {
            fieldSchema.setEnum(values);
            fieldSchema.setExample(values.get(0));
        }
    }
}
