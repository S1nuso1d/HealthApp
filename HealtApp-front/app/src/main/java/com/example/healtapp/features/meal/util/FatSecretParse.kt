package com.example.healtapp.features.meal.util



import com.google.gson.JsonArray

import com.google.gson.JsonObject



data class FatSecretFoodHit(

    val foodId: String,

    val name: String,

    val description: String?,

)



data class FatSecretServingOption(

    val servingId: String?,

    val description: String,

    val calories: Float?,

    val proteinG: Float?,

    val fatG: Float?,

    val carbsG: Float?,

)



data class FatSecretServingNutrition(

    val foodName: String,

    val calories: Float?,

    val proteinG: Float?,

    val fatG: Float?,

    val carbsG: Float?,

)



data class FatSecretFoodDetail(

    val foodName: String,

    val servings: List<FatSecretServingOption>,

)



object FatSecretParse {



    fun unwrap(root: JsonObject): JsonObject {

        val payload = when {

            root.has("raw") && root.get("raw").isJsonObject -> root.getAsJsonObject("raw")

            else -> root

        }

        val resp = payload.get("response")

        return if (resp != null && resp.isJsonObject) resp.asJsonObject else payload

    }



    fun parseSearchHits(root: JsonObject): List<FatSecretFoodHit> {

        val u = unwrap(root)

        val foods = u.getAsJsonObject("foods") ?: return emptyList()

        val foodEl = foods.get("food") ?: return emptyList()

        val arr: JsonArray = when {

            foodEl.isJsonArray -> foodEl.asJsonArray

            foodEl.isJsonObject -> {

                val a = JsonArray()

                a.add(foodEl)

                a

            }

            else -> return emptyList()

        }

        val out = mutableListOf<FatSecretFoodHit>()

        for (e in arr) {

            if (!e.isJsonObject) continue

            val o = e.asJsonObject

            val id = stringField(o, "food_id") ?: continue

            val name = stringField(o, "food_name") ?: continue

            val desc = stringField(o, "food_description")

            out.add(FatSecretFoodHit(foodId = id, name = name, description = desc))

        }

        return out

    }



    fun extractFoodIdFromBarcodeResponse(root: JsonObject): String? {

        val u = unwrap(root)

        val fid = u.get("food_id") ?: return null

        return when {

            fid.isJsonObject -> stringField(fid.asJsonObject, "value")

            fid.isJsonPrimitive && fid.asJsonPrimitive.isString -> fid.asString

            else -> null

        }

    }



    fun parseFoodDetail(root: JsonObject): FatSecretFoodDetail? {

        val u = unwrap(root)

        val food = u.getAsJsonObject("food") ?: return null

        val name = stringField(food, "food_name") ?: return null

        val servings = parseServings(food)

        return FatSecretFoodDetail(foodName = name, servings = servings)

    }



    fun parseFoodGet(root: JsonObject): FatSecretServingNutrition? {

        val detail = parseFoodDetail(root) ?: return null

        val serving = detail.servings.firstOrNull()

        return FatSecretServingNutrition(

            foodName = detail.foodName,

            calories = serving?.calories,

            proteinG = serving?.proteinG,

            fatG = serving?.fatG,

            carbsG = serving?.carbsG,

        )

    }



    fun parseServings(food: JsonObject): List<FatSecretServingOption> {

        val servings = food.getAsJsonObject("servings") ?: return emptyList()

        val servEl = servings.get("serving") ?: return emptyList()

        val arr: JsonArray = when {

            servEl.isJsonArray -> servEl.asJsonArray

            servEl.isJsonObject -> JsonArray().also { it.add(servEl) }

            else -> return emptyList()

        }

        val out = mutableListOf<FatSecretServingOption>()

        for (e in arr) {

            if (!e.isJsonObject) continue

            val o = e.asJsonObject

            val desc = stringField(o, "serving_description")

                ?: stringField(o, "measurement_description")

                ?: "Порция"

            out.add(

                FatSecretServingOption(

                    servingId = stringField(o, "serving_id"),

                    description = desc,

                    calories = floatField(o, "calories"),

                    proteinG = floatField(o, "protein"),

                    fatG = floatField(o, "fat"),

                    carbsG = floatField(o, "carbohydrate"),

                ),

            )

        }

        return out

    }



    private fun stringField(o: JsonObject, key: String): String? {

        val el = o.get(key) ?: return null

        return when {

            el.isJsonPrimitive -> {

                val p = el.asJsonPrimitive

                when {

                    p.isString -> p.asString

                    p.isNumber -> p.asNumber.toString()

                    else -> null

                }

            }

            el.isJsonObject -> {

                val nested = el.asJsonObject

                stringField(nested, "value")

                    ?: nested.keySet()

                        .firstOrNull { k -> nested.get(k)?.isJsonPrimitive == true }

                        ?.let { k -> nested.get(k)?.asJsonPrimitive?.asString }

            }

            else -> null

        }

    }



    private fun floatField(o: JsonObject, key: String): Float? {

        val el = o.get(key) ?: return null

        when {

            el.isJsonPrimitive -> {

                val p = el.asJsonPrimitive

                return when {

                    p.isNumber -> p.asFloat

                    p.isString -> p.asString.replace(",", ".").toFloatOrNull()

                    else -> null

                }

            }

            el.isJsonObject -> {

                val inner = el.asJsonObject.get("value") ?: return null

                if (inner.isJsonPrimitive) {

                    val p = inner.asJsonPrimitive

                    return when {

                        p.isNumber -> p.asFloat

                        p.isString -> p.asString.replace(",", ".").toFloatOrNull()

                        else -> null

                    }

                }

            }

        }

        return null

    }

}

