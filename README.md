# COVID-19-Assignment
covid data to analyse the confirmed, recovered and recovery ratio

# COVID-19 Time Series Data Query Application

This command-line Java application allows you to query COVID-19 confirmed and recovered case statistics from time series datasets (sourced from Johns Hopkins University).

## Features

- Query global, continent, country, or province/state level statistics for any date.
- Get confirmed cases, recovered cases, and recovery ratio.
- Flexible query options: filter by date, continent, country, province/state, and type (confirmed/recovered/both).
- Simple, interactive command-line interface.
- No external libraries or frameworks required (pure Java).

## Requirements

- Java 8 or higher.
- Three CSV files in the same directory as the program:
  - `covid_confirmed.csv`
  - `covid_recovered.csv`
  - `countries_to_continent.csv`
