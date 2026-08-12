# Notes

General notes section

## Things to make architecture notes of

- Availability architecture
  - Availability rules
  - Slot calculation
- Capacity - resource or availability slot
- Decoupling JPA entities - impact of removing relationships, port/adapter interfaces
- From a modular monolith - how would you go about "promoting" modules to micro services?
  - What would be each level of independence? i.e. seperate maven package, then what?
  - What would impact be on integration tests when extracting a module?
- JWT + localStorage VS JWT + Refresh token + HTTPOnly session cookie
  - JWT + Refresh token - method requires revoked JWT storage to implement logout functionality