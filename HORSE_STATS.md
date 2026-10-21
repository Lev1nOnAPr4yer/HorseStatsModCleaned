# How horse stats and horse breeding work

A horse has three stats that matter: **health**, **jump strength** and **speed**. They are rolled separately, so a
horse can have great speed and poor health. This is for Java Edition 26.3, checked against the game's code.

| Stat | Range | Average | In blocks |
|---|---|---|---|
| Health | 15 – 30 HP | 22.5 | (15 HP = 7.5 hearts) |
| Jump strength | 0.4 – 1.0 | 0.7 | 1.15 – 5.92 blocks (0.7 = 3.12 blocks) |
| Speed | 0.1125 – 0.3375 | 0.225 | 4.86 – 14.57 blocks/s (× 43.17) |

Jump strength is the upward velocity of the jump, so height grows faster than strength. Speed in blocks per second is
the speed value × 43.17 on flat ground.

## Naturally spawned horses

Each stat is rolled once, when the horse spawns, and never changes:

```
health = 15 + randomInt(0..7) + randomInt(0..8)
jump   = 0.4 + 0.2·r1 + 0.2·r2 + 0.2·r3
speed  = (0.45 + 0.3·r1 + 0.3·r2 + 0.3·r3) × 0.25
```

`r1`, `r2` and `r3` are fresh random numbers between 0 and 1 (never exactly 1).

**Distribution.** Every stat is the sum of a few random rolls, so the results form a bell curve around the middle of the
range. Average horses are common and the extremes are very rare, because a top value needs every roll to be high at once.

- Health only takes whole numbers. 22 and 23 HP are the most common (8 in 72 each), 30 HP is 1 in 72, and 15 HP is
  also 1 in 72.
- Jump strength and speed are not rounded to steps. They can be any value in their range, and two horses can
  differ by a tiny amount.

| Wild horses at or above | Speed | Jump |
|---|---|---|
| Top 10% | 0.274 (11.8 blocks/s) | 0.831 (4.26 blocks) |
| Top 1% | 0.308 (13.3 blocks/s) | 0.922 (5.12 blocks) |
| Top 0.1% | 0.324 (14.0 blocks/s) | 0.964 (5.54 blocks) |

Because of the curve, "70% of the way up the range" is much better than 70% of horses: a speed that is 70% of the way
from the minimum to the maximum beats about 88% of wild horses.

## Foals

When two horses breed, the foal's three stats are rolled independently from its parents' values. For one stat, with
parents `A` and `B` and the stat's minimum and maximum:

```
margin  = 0.15 × (max − min)
range   = |A − B| + 2 × margin
average = (A + B) / 2
quality = (r1 + r2 + r3) / 3 − 0.5          // between −0.5 and +0.5, bell-shaped
foal    = average + range × quality
```

If the foal lands above the maximum, it bounces back inside by the same distance (`max − (foal − max)`), and the
same happens under the minimum. Only the parents' base values count: potion effects are not passed down.

What this means:

- **The foal is centered on the average of its parents.** On average it is exactly as good as the mean of the two
  parents, not better. Breeding two good horses gives a good foal, but you only get better ones by keeping the lucky
  foals and breeding those again.
- **Similar parents give similar foals.** Two identical parents give a foal within ±15% of the stat's range around
  them. Parents far apart give a much wider spread, because the range grows by the size of the gap between them.
- **Nothing is rounded.** Health is no longer a whole number: a foal can have 26.84 HP.
- **The limits can't be passed.** The bounce-back means that a foal never goes above the maximum or under the
  minimum. Reaching exactly the maximum or the minimum has no chance in practice, but you can get very close.
- **Near the top the foal usually can't beat its parents.** The chance that a foal beats two identical parents on
  one stat:

  | Parents at (share of the range) | Chance the foal is higher |
  |---|---|
  | 50% to 90% | 50% |
  | 95% | 48% |
  | 98% | 28% |
  | 99.5% | 7.5% |
  | 100% (maximum) | 0% |

  Two parents at the maximum give foals that land between 85% and 100% of the range, around 96% in the middle.
- **Foals use the same ranges as wild horses**, and their stats are fixed once they are born.
