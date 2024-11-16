package com.pokemon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masser.jdbc.Connector;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class PokeApiImporter {
    private final Connector connector;

    public PokeApiImporter(Connector connector) {
        this.connector = connector;
    }

    private JsonObject fetchFromApi(String endpoint) throws Exception {
        URL url = new URL("https://pokeapi.co/api/v2/" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != 200) {
            throw new Exception("Failed to fetch data: " + connection.getResponseMessage());
        }

        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
        return JsonParser.parseReader(reader).getAsJsonObject();
    }

    public void insertPokemon(int id) throws Exception {
        int pokedexEntry;
        String name;
        String type1 = null;
        String type2 = null;
        int generation;
        int isLegendary = 0;
        int isMythical = 0;

        JsonObject pokemon = fetchFromApi("pokemon/" + id);

        pokedexEntry = pokemon.get("id").getAsInt();
        name = pokemon.get("name").getAsString();
        JsonArray types = pokemon.getAsJsonArray("types");

        for (int i = 0; i < types.size(); i++) {
            JsonObject typeObject = types.get(i).getAsJsonObject();
            String typeName = typeObject.getAsJsonObject("type").get("name").getAsString();
            int slot = typeObject.get("slot").getAsInt();
            if (slot == 1) {
                type1 = typeName;
            } else {
                type2 = typeName;
            }
        }

        generation = this.getGeneration(pokedexEntry);
        if (this.isLegendary(pokedexEntry)) {
            isLegendary = 1;
        }
        if (this.isMythical(pokedexEntry)) {
            isMythical = 1;
        }

        System.out.println("Inserting Pokémon: " + name);
        connector.executeUpdate(
                "INSERT INTO pokemon (pokedex_entry, name, type1, type2, generation, is_legendary, is_mythical) VALUES (" +
                "'" + pokedexEntry + "'" + ", " +

                        "'" +name + "'" + ", " +

                        "'" +type1 + "'" + ", " +

                        "'" +type2 + "'" + ", " +

                        "'" +generation + "'" + ", " +

                        "'" +isLegendary + "'" + ", " +

                        "'" +isMythical + "'" + ")"
        );
    }


    private boolean isLegendary(int pokedexEntry) throws Exception {
        JsonObject pokemonSpecie = fetchFromApi("pokemon-species/" + pokedexEntry);
        return pokemonSpecie.get("is_legendary").getAsBoolean();
    }

    private boolean isMythical(int pokedexEntry) throws Exception {
        JsonObject pokemonSpecie = fetchFromApi("pokemon-species/" + pokedexEntry);
        return pokemonSpecie.get("is_mythical").getAsBoolean();
    }

    private int getGeneration(int pokedexEntry) throws Exception {
        JsonObject pokemonSpecie = fetchFromApi("pokemon-species/" + pokedexEntry);

        JsonObject generation = pokemonSpecie.getAsJsonObject("generation");
        String generationName = generation.get("name").getAsString();
        String generationRoman = generationName.split("-")[1];
        return romanToInt(generationRoman);
    }

    private int romanToInt(String roman) {
        Map<Character, Integer> romanMap = new HashMap<>();
        romanMap.put('i', 1);
        romanMap.put('v', 5);
        romanMap.put('x', 10);

        int result = 0;
        int prevValue = 0;

        for (int i = roman.length() - 1; i >= 0; i--) {
            int currentValue = romanMap.get(roman.charAt(i));
            if (currentValue < prevValue) {
                result -= currentValue;
            } else {
                result += currentValue;
            }
            prevValue = currentValue;
        }
        return result;
    }
}
