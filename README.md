# Prekladaci asistent

1. Vyrob CSV subor `foo.csv` s hodnotami, napr.

```
mandate, mandat
without mandate, ohne mandat
"something with a ,", "das hier hat eine komma, "
hier is no comma!, hier is kei komma!
```

2. Zbuilduj asistenta s `./gradlew`

3. Rozbal `build/distributions/prekladaciasistent-1.0-SNAPSHOT.zip`

4. Vyrob index: `./pa -i foo.csv`

5. Vyhladaj: `./pa -s hier`
