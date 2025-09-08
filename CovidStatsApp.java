import java.io.*;
import java.util.*;

public class CovidStatsApp {
    static class CovidRecord {
        String country;
        String province;
        String date;
        int count;

        CovidRecord(String country, String province, String date, int count) {
            this.country = country;
            this.province = province;
            this.date = date;
            this.count = count;
        }
    }

    // Mapping from country to continent
    static class CountryToContinent {
        Map<String, String> countryToContinent = new HashMap<>();

        CountryToContinent(String csvPath) throws IOException {
            try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
                String line;
                br.readLine(); 
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        countryToContinent.put(parts[0].trim().toLowerCase(), parts[1].trim());
                    }
                }
            }
        }

        String getContinent(String country) {
            return countryToContinent.getOrDefault(country.toLowerCase(), "Unknown");
        }
    }

    static class CovidData {
        // here we can store date -> country -> province -> count using Map
        Map<String, Map<String, Map<String, Integer>>> confirmed = new HashMap<>();
        Map<String, Map<String, Map<String, Integer>>> recovered = new HashMap<>();

        Set<String> allDates = new HashSet<>();
        Set<String> allCountries = new HashSet<>();
        Set<String> allProvinces = new HashSet<>();

        CovidData(String confirmedCsv, String recoveredCsv) throws IOException {
            loadCsv(confirmedCsv, confirmed);
            loadCsv(recoveredCsv, recovered);
        }

        // Loads CSV file into the provided map of each row 
        private void loadCsv(String csvPath, Map<String, Map<String, Map<String, Integer>>> map) throws IOException {
            try (BufferedReader buffer = new BufferedReader(new FileReader(csvPath))) {
                String header = buffer.readLine();
                String[] columns = header.split(",");
                List<String> dateColumns = new ArrayList<>();
                for (int column = 4; column < columns.length; column++) {
                    dateColumns.add(columns[column].trim());
                }

                String line;
                while ((line = buffer.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    String country = parts[1].trim();
                    String province = parts[0].trim();
                    allCountries.add(country.toLowerCase());
                    allProvinces.add(province.toLowerCase());
					int numDates = Math.min(dateColumns.size(), parts.length - 4);
                    for (int column = 4; column < numDates; column++) {
                        String date = dateColumns.get(column - 4);
                        allDates.add(date);
                        int count = 0;
                        try {
                            count = Integer.parseInt(parts[column].trim().isEmpty() ? "0" : parts[column].trim());
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                        map.computeIfAbsent(date, d -> new HashMap<>())
                            .computeIfAbsent(country.toLowerCase(), c -> new HashMap<>())
                            .put(province.toLowerCase(), count);
                    }
                }
            }
        }

        // Returns total for the whole world for given date
        int getTotal(Map<String, Map<String, Map<String, Integer>>> map, String date) {
            int total = 0;
            Map<String, Map<String, Integer>> byCountry = map.get(date);
            if (byCountry != null) {
                for (Map<String, Integer> byProvince : byCountry.values()) {
                    for (int count : byProvince.values()) total += count;
                }
            }
            return total;
        }

        // Returns total for a continent given date 
        int getTotalByContinent(Map<String, Map<String, Map<String, Integer>>> map, String date, CountryToContinent ctc, String continent) {
            int total = 0;
            Map<String, Map<String, Integer>> byCountry = map.get(date);
            if (byCountry != null) {
                for (String country : byCountry.keySet()) {
                    if (ctc.getContinent(country).equalsIgnoreCase(continent)) {
                        for (int count : byCountry.get(country).values()) total += count;
                    }
                }
            }
            return total;
        }

        // Returns total for a country given date
        int getTotalByCountry(Map<String, Map<String, Map<String, Integer>>> map, String date, String country) {
            int total = 0;
            Map<String, Map<String, Integer>> byCountry = map.get(date);
            if (byCountry != null) {
                Map<String, Integer> byProvince = byCountry.get(country.toLowerCase());
                if (byProvince != null) {
                    for (int count : byProvince.values()){
						total += count;
					}
                }
            }
            return total;
        }

        // Returns total for specified province/state for a given date
        int getTotalByCountryProvince(Map<String, Map<String, Map<String, Integer>>> map, String date, String country, String province) {
            Map<String, Map<String, Integer>> byCountry = map.get(date);
            if (byCountry != null) {
                Map<String, Integer> byProvince = byCountry.get(country.toLowerCase());
                if (byProvince != null) {
                    return byProvince.getOrDefault(province.toLowerCase(), 0);
                }
            }
            return 0;
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);
        
        String confirmedCsv = "covid_confirmed.csv";
        String recoveredCsv = "covid_recovered.csv";
        String ctcCsv = "countries_to_continent.csv";

        CovidData data = new CovidData(confirmedCsv, recoveredCsv);
        CountryToContinent ctc = new CountryToContinent(ctcCsv);

        System.out.println("COVID-19 Stats CLI");
        System.out.println("Type your query. For example:\n"
                + "date=4/16/20 or 04-16-2020, country=united kingdom\n"
                + "date=4/19/20 or 04-19-2020, country=china, state=shanghai\n"
                + "date=1/1/21 or 01-01-2021, continent=europe\n"
                + "You may also specify 'type=confirmed', 'type=recovered', or 'type=both'.\n"
                + "Press Enter to exit.");
        while (true) {
            System.out.print("\nEnter Query: ");
            String input = scan.nextLine().trim();
            if (input.isEmpty()) break;

            String date = null, continent = null, country = null, province = null, type = "both";
            for (String part : input.split(",")) {
                String[] value = part.trim().split("=", 2);
                if (value.length == 2) {
                    String val1 = value[0].trim().toLowerCase(), val2 = value[1].trim().toLowerCase();
                    switch (val1) {
                        case "date": date = val2; break;
                        case "continent": continent = val2; break;
                        case "country": country = val2; break;
                        case "state":
                        case "province": province = val2; break;
                        case "type": type = val2; break;
                    }
                }
            }
            if (date == null) {
                System.out.println("Please specify date parameter.");
                continue;
            }
            int confirmed = 0, recovered = 0;
            if (continent != null) {
                confirmed = data.getTotalByContinent(data.confirmed, date, ctc, continent);
                recovered = data.getTotalByContinent(data.recovered, date, ctc, continent);
            } else if (country != null && province != null) {
                confirmed = data.getTotalByCountryProvince(data.confirmed, date, country, province);
                recovered = data.getTotalByCountryProvince(data.recovered, date, country, province);
            } else if (country != null) {
                confirmed = data.getTotalByCountry(data.confirmed, date, country);
                recovered = data.getTotalByCountry(data.recovered, date, country);
            } else {
                confirmed = data.getTotal(data.confirmed, date);
                recovered = data.getTotal(data.recovered, date);
            }
            double recoveryRatio = (confirmed == 0) ? 0.0 : ((double) recovered / confirmed);

            StringBuilder builder = new StringBuilder();
            if (type.equals("confirmed")) builder.append("confirmed=").append(confirmed);
            else if (type.equals("recovered")) builder.append("recovered=").append(recovered);
            else builder.append("recovered=").append(recovered).append(", confirmed=").append(confirmed);
            builder.append(", recovery_ratio=").append(String.format("%.2f", recoveryRatio));
            System.out.println(builder.toString());
        }
    }
}