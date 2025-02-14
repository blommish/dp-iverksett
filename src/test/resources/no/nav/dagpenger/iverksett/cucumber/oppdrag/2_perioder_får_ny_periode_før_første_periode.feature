# language: no
# encoding: UTF-8

Egenskap: 2 perioder får en ny periode

  Scenario: Har 2 perioder og får en endring før første periode

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato   | Til dato   | Beløp |
      | 1            | 01.02.2021 | 30.04.2021 | 700   |
      | 1            | 01.05.2021 | 31.07.2021 | 900   |
      | 2            | 01.01.2021 | 31.01.2021 | 500   |
      | 2            | 01.05.2021 | 31.07.2021 | 900   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato   | Til dato   | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 01.02.2021 | 30.04.2021 |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 01.05.2021 | 31.07.2021 |             | 900   | NY           | Nei        | 1          | 0                  |
      | 2            | 01.01.2021 | 31.01.2021 |             | 500   | ENDR         | Nei        | 2          | 1                  |
      | 2            | 01.05.2021 | 31.07.2021 |             | 900   | ENDR         | Nei        | 3          | 2                  |
