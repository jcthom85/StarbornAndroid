# constants.py
# Base stat constants (percentages expressed as floats for calculations)
BASE_ACC = 95.0      # Base accuracy in percent
BASE_CRT = 5.0       # Base critical chance in percent
BASE_EVA = 5.0       # Base evasion in percent

# Primary-to-derived multipliers
ATK_STR_MULT = 2.0   # Attack per point of Strength
DEF_VIT_MULT = 1.5   # Defense per point of Vitality
HP_PER_VIT   = 10    # Max HP per point of Vitality
SPD_AGI_MULT = 1.2   # Speed per point of Agility

EVA_PER_AGI  = 0.15  # Evasion % per point of Agility
ACC_PER_FOC  = 0.20  # Accuracy % per point of Focus
CRT_PER_FOC  = 0.15  # Crit % per point of Focus
RES_PER_FOC  = 0.10  # Resist (general) per point of Focus

DROP_PER_LUCK = 0.25 # Drop rate % per point of Luck

CRIT_DAMAGE_MULT = 2.0  # Critical hits damage multiplier (e.g., 2.0 = double damage)
