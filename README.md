# brreg-unnderenheter

Oppgave i forbindelse med oppstart i CRM plattform.

## Introduksjon:

For begge oppgavene forventes følgende ramme;

-	Koding skal gjøres i Kotlin
-	Bygg skal gjøres med Gradle
-	Det skal utvikles hensiktsmessig automatiske tester som kjøres under bygg
-	Program skal kjøres i NAIS preprod_sbs, bruker kafka preprod 
-	Automatisk deploy til NAIS preprod_sbs ved commit til github – bruk «Internal» repo 
-	Alle kafka hendelser skal være definert ihht til protobuf 3.0 skjema

## Oppgave 1:

Lag et program som gjør følgende;
1.	Leser inn en kafka compaction log som inneholder alle underenheter
2.	Laster ned underenheter fra Brønnøysund hver natt kl. 05:30 https://data.brreg.no/enhetsregisteret/api/underenheter/lastned
3.	Publiserer underenheter som har endret seg tilbake til kafka compaction log (samme som i punkt 1) 


