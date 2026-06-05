package com.national.utility.billing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.national.utility.billing.dto.common.LocationAddressDto;
import com.national.utility.billing.dto.common.LocationSelectionDto;
import com.national.utility.billing.dto.response.LocationPickerResponse;
import com.national.utility.billing.dto.response.LocationSearchResult;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.model.embeddable.LocationAddress;
import com.national.utility.billing.model.location.LocationRecord;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final String KEY_SEPARATOR = "|";

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${app.locations.file:classpath:locations.json}")
    private String locationsFile;

    @Value("${app.locations.swagger-village-enum-limit:8000}")
    private int swaggerVillageEnumLimit;

    @Getter
    private boolean loaded;

    private final List<String> provinces = new ArrayList<>();
    private final Set<String> allDistricts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> allSectors = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> allCells = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> allVillages = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Set<String>> districtsByProvince = new LinkedHashMap<>();
    private final Map<String, Set<String>> sectorsByDistrict = new LinkedHashMap<>();
    private final Map<String, Set<String>> cellsBySector = new LinkedHashMap<>();
    private final Map<String, Set<String>> villagesByCell = new LinkedHashMap<>();
    private final Map<String, LocationAddress> canonicalAddressByPath = new HashMap<>();
    private final List<LocationSearchEntry> searchIndex = new ArrayList<>();

    private record LocationSearchEntry(String label, String searchText, LocationAddress address) {}

    @PostConstruct
    public void loadLocations() throws IOException {
        Resource resource = resourceLoader.getResource(locationsFile);
        try (InputStream inputStream = resource.getInputStream()) {
            List<LocationRecord> records = objectMapper.readValue(
                    inputStream, new TypeReference<List<LocationRecord>>() {});

            Set<String> provinceSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            for (LocationRecord record : records) {
                if (!hasText(record.getProvinceName()) || !hasText(record.getDistrictName())
                        || !hasText(record.getSectorName()) || !hasText(record.getCellName())
                        || !hasText(record.getVillageName())) {
                    continue;
                }

                String province = record.getProvinceName().trim();
                String district = record.getDistrictName().trim();
                String sector = record.getSectorName().trim();
                String cell = record.getCellName().trim();
                String village = record.getVillageName().trim();

                provinceSet.add(province);
                allDistricts.add(district);
                allSectors.add(sector);
                allCells.add(cell);
                allVillages.add(village);
                districtsByProvince.computeIfAbsent(normalize(province), k -> new LinkedHashSet<>()).add(district);

                String districtKey = pathKey(province, district);
                sectorsByDistrict.computeIfAbsent(districtKey, k -> new LinkedHashSet<>()).add(sector);

                String sectorKey = pathKey(province, district, sector);
                cellsBySector.computeIfAbsent(sectorKey, k -> new LinkedHashSet<>()).add(cell);

                String cellKey = pathKey(province, district, sector, cell);
                villagesByCell.computeIfAbsent(cellKey, k -> new LinkedHashSet<>()).add(village);

                String fullKey = pathKey(province, district, sector, cell, village);
                LocationAddress canonical = LocationAddress.builder()
                        .province(province)
                        .district(district)
                        .sector(sector)
                        .cell(cell)
                        .village(village)
                        .build();
                if (canonicalAddressByPath.putIfAbsent(fullKey, canonical) == null) {
                    String label = formatLabel(canonical);
                    searchIndex.add(new LocationSearchEntry(label, label.toLowerCase(Locale.ROOT), canonical));
                }
            }

            provinces.clear();
            provinces.addAll(provinceSet);
            loaded = true;
            log.info("Loaded {} Rwanda location records ({} provinces, {} districts, {} villages for Swagger dropdowns)",
                    records.size(), provinces.size(), allDistricts.size(), allVillages.size());
        }
    }

    public List<String> getProvinces() {
        ensureLoaded();
        return List.copyOf(provinces);
    }

    public List<String> getAllDistricts() {
        ensureLoaded();
        return sortedList(allDistricts);
    }

    public List<String> getAllSectors() {
        ensureLoaded();
        return sortedList(allSectors);
    }

    public List<String> getAllCells() {
        ensureLoaded();
        return sortedList(allCells);
    }

    public List<String> getAllVillages() {
        ensureLoaded();
        return sortedList(allVillages);
    }

    public int getSwaggerVillageEnumLimit() {
        return swaggerVillageEnumLimit;
    }

    public LocationAddress resolveSelection(LocationSelectionDto selection) {
        if (selection == null) {
            throw new BusinessException("Location is required");
        }
        return resolveAddress(selection.toAddressDto());
    }

    public List<String> getDistricts(String province) {
        ensureLoaded();
        requireText(province, "Province");
        Set<String> districts = districtsByProvince.get(normalize(province));
        if (districts == null) {
            throw new BusinessException("Unknown province: " + province);
        }
        return sortedList(districts);
    }

    public List<String> getSectors(String province, String district) {
        ensureLoaded();
        requireText(province, "Province");
        requireText(district, "District");
        Set<String> sectors = sectorsByDistrict.get(pathKey(province, district));
        if (sectors == null) {
            throw new BusinessException("Unknown province/district combination");
        }
        return sortedList(sectors);
    }

    public List<String> getCells(String province, String district, String sector) {
        ensureLoaded();
        requireText(province, "Province");
        requireText(district, "District");
        requireText(sector, "Sector");
        Set<String> cells = cellsBySector.get(pathKey(province, district, sector));
        if (cells == null) {
            throw new BusinessException("Unknown province/district/sector combination");
        }
        return sortedList(cells);
    }

    public List<String> getVillages(String province, String district, String sector, String cell) {
        ensureLoaded();
        requireText(province, "Province");
        requireText(district, "District");
        requireText(sector, "Sector");
        requireText(cell, "Cell");
        Set<String> villages = villagesByCell.get(pathKey(province, district, sector, cell));
        if (villages == null) {
            throw new BusinessException("Unknown province/district/sector/cell combination");
        }
        return sortedList(villages);
    }

    public boolean isValidAddress(String province, String district, String sector, String cell, String village) {
        if (!hasText(province) || !hasText(district) || !hasText(sector) || !hasText(cell) || !hasText(village)) {
            return false;
        }
        return canonicalAddressByPath.containsKey(
                pathKey(province.trim(), district.trim(), sector.trim(), cell.trim(), village.trim()));
    }

    public boolean isValidAddress(LocationAddressDto address) {
        if (address == null) {
            return false;
        }
        return isValidAddress(
                address.getProvince(), address.getDistrict(), address.getSector(),
                address.getCell(), address.getVillage());
    }

    public LocationAddress resolveAddress(LocationAddressDto address) {
        if (address == null) {
            throw new BusinessException("Address is required");
        }
        String key = pathKey(
                address.getProvince(), address.getDistrict(), address.getSector(),
                address.getCell(), address.getVillage());
        LocationAddress canonical = canonicalAddressByPath.get(key);
        if (canonical == null) {
            throw new BusinessException(
                    "Invalid address. Use GET /api/locations/search?keyword=... and copy the address object.");
        }
        return canonical;
    }

    public List<LocationSearchResult> search(String keyword, int limit) {
        ensureLoaded();
        requireText(keyword, "Keyword");
        if (limit < 1 || limit > 100) {
            throw new BusinessException("Limit must be between 1 and 100");
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        List<LocationSearchResult> results = new ArrayList<>();

        for (LocationSearchEntry entry : searchIndex) {
            if (entry.searchText().contains(normalizedKeyword)) {
                results.add(LocationSearchResult.builder()
                        .label(entry.label())
                        .address(LocationAddressDto.fromEntity(entry.address()))
                        .build());
                if (results.size() >= limit) {
                    break;
                }
            }
        }

        return results;
    }

    public LocationPickerResponse picker(String province, String district, String sector, String cell, String village) {
        ensureLoaded();

        LocationAddressDto selection = LocationAddressDto.builder()
                .province(trimOrNull(province))
                .district(trimOrNull(district))
                .sector(trimOrNull(sector))
                .cell(trimOrNull(cell))
                .village(trimOrNull(village))
                .build();

        LocationPickerResponse.PickerOptions.PickerOptionsBuilder optionsBuilder =
                LocationPickerResponse.PickerOptions.builder()
                        .provinces(getProvinces());

        String nextStep;

        if (!hasText(province)) {
            nextStep = "Easiest: use GET /api/locations/search?keyword=YourVillage. "
                    + "Or pick a province and call /api/locations/picker?province=KIGALI";
            return LocationPickerResponse.builder()
                    .selection(selection)
                    .options(optionsBuilder.build())
                    .complete(false)
                    .nextStep(nextStep)
                    .build();
        }

        optionsBuilder.districts(getDistricts(province));

        if (!hasText(district)) {
            nextStep = "Pick a district and add &district=Nyarugenge";
            return buildPickerResponse(selection, optionsBuilder.build(), false, null, nextStep);
        }

        optionsBuilder.sectors(getSectors(province, district));

        if (!hasText(sector)) {
            nextStep = "Pick a sector and add &sector=Gitega";
            return buildPickerResponse(selection, optionsBuilder.build(), false, null, nextStep);
        }

        optionsBuilder.cells(getCells(province, district, sector));

        if (!hasText(cell)) {
            nextStep = "Pick a cell and add &cell=Akabahizi";
            return buildPickerResponse(selection, optionsBuilder.build(), false, null, nextStep);
        }

        optionsBuilder.villages(getVillages(province, district, sector, cell));

        if (!hasText(village)) {
            nextStep = "Pick a village and add &village=Gihanga — or use /search for one-step lookup";
            return buildPickerResponse(selection, optionsBuilder.build(), false, null, nextStep);
        }

        LocationAddress resolved = resolveAddress(selection);
        LocationAddressDto resolvedDto = LocationAddressDto.fromEntity(resolved);
        nextStep = "Address is complete. Copy resolvedAddress into Customer/Invite address field.";

        return buildPickerResponse(selection, optionsBuilder.build(), true, resolvedDto, nextStep);
    }

    private LocationPickerResponse buildPickerResponse(
            LocationAddressDto selection,
            LocationPickerResponse.PickerOptions options,
            boolean complete,
            LocationAddressDto resolvedAddress,
            String nextStep) {
        return LocationPickerResponse.builder()
                .selection(selection)
                .options(options)
                .complete(complete)
                .resolvedAddress(resolvedAddress)
                .nextStep(nextStep)
                .build();
    }

    private static String formatLabel(LocationAddress address) {
        return String.join(" > ",
                address.getProvince(),
                address.getDistrict(),
                address.getSector(),
                address.getCell(),
                address.getVillage());
    }

    private static String trimOrNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private void ensureLoaded() {
        if (!loaded) {
            throw new BusinessException("Location data is not loaded yet");
        }
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String pathKey(String... parts) {
        return String.join(KEY_SEPARATOR, Arrays.stream(parts).map(LocationService::normalize).toArray(String[]::new));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void requireText(String value, String field) {
        if (!hasText(value)) {
            throw new BusinessException(field + " is required");
        }
    }

    private static List<String> sortedList(Set<String> values) {
        List<String> list = new ArrayList<>(values);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }
}
