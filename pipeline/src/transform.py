# pipeline/src/transform.py

# This script transforms the raw financial transactions dataset and
# produces a clean, normalised version.

import pandas as pd
import pandera as pa
from pandera import Column, DataFrameSchema, Check
import hashlib
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

CATEGORY_NORMALISATION_MAP = {
    'supermercado':   'Supermercado',
    'restaurantes':   'Restaurantes',
    'restaurante':    'Restaurantes',
    'transportes':    'Transportes',
    'transporte':     'Transportes',
    'saude':          'Saúde',
    'saúde':          'Saúde',
    'lazer':          'Lazer',
    'habitacao':      'Habitação',
    'habitação':      'Habitação',
    'servicos':       'Serviços',
    'serviços':       'Serviços',
    'vestuario':      'Vestuário',
    'vestuário':      'Vestuário',
    'educacao':       'Educação',
    'educação':       'Educação',
    'investimentos':  'Investimentos',
    'invest':         'Investimentos',
    'outros':         'Outros',
}

VALID_CATEGORIES = set(CATEGORY_NORMALISATION_MAP.values())

RAW_SCHEMA = DataFrameSchema({
    'description':      Column(str, nullable=False),
    'amount':           Column(float, Check.greater_than(0)),
    'type':             Column(str, Check.isin(['DEBIT', 'CREDIT'])),
    'transaction_date': Column(str, nullable=False),
    'category':         Column(str, nullable=True),
})

CLEAN_SCHEMA = DataFrameSchema({
    'description':      Column(str, nullable=False),
    'amount':           Column(float, Check.greater_than(0)),
    'type':             Column(str, Check.isin(['DEBIT', 'CREDIT'])),
    'transaction_date': Column(pa.DateTime, nullable=False),
    'category':         Column(str, Check.isin(list(VALID_CATEGORIES))),
    'row_hash':         Column(str, nullable=False),
})


def normalise_category(value) -> str:
    if pd.isna(value) or str(value).strip() == '':
        return 'Outros'
    normalised = str(value).strip().lower()
    # removes accents and special characters for better matching
    normalised = (normalised
        .replace('ã', 'a').replace('á', 'a').replace('à', 'a')
        .replace('é', 'e').replace('ê', 'e')
        .replace('í', 'i').replace('ó', 'o').replace('ô', 'o')
        .replace('ú', 'u').replace('ç', 'c'))
    return CATEGORY_NORMALISATION_MAP.get(normalised, 'Outros')


def compute_hash(row: pd.Series) -> str:
    value = f"{row['description']}|{row['amount']}|{row['type']}|{row['transaction_date']}"
    return hashlib.md5(value.encode()).hexdigest()


def transform(df: pd.DataFrame) -> tuple[pd.DataFrame, dict]:
    metrics = {}
    initial_count = len(df)
    logger.info(f"Início da transformação: {initial_count} linhas")

    # Ray schema validation on raw input to catch major issues
    try:
        RAW_SCHEMA.validate(df, lazy=True)
    except pa.errors.SchemaErrors as e:
        logger.warning(f"Erros de validação no input: {len(e.failure_cases)} casos")

    # 1. Remove lines with nulls in required fields
    df = df.dropna(subset=['description', 'amount', 'type', 'transaction_date'])
    metrics['dropped_null'] = initial_count - len(df)

    # 2. Categories normalisation
    df = df.copy()
    df['category'] = df['category'].apply(normalise_category)

    # 3. Convert dates and sort
    df['transaction_date'] = pd.to_datetime(df['transaction_date'], errors='coerce')
    invalid_dates = df['transaction_date'].isna().sum()
    df = df.dropna(subset=['transaction_date'])
    metrics['dropped_invalid_date'] = int(invalid_dates)
    df = df.sort_values('transaction_date').reset_index(drop=True)

    # 4. Assure correct types and positive amounts
    df['amount'] = pd.to_numeric(df['amount'], errors='coerce').round(2)
    df = df.dropna(subset=['amount'])
    df = df[df['amount'] > 0]

    # 5. Remove duplicates by hash
    df['row_hash'] = df.apply(compute_hash, axis=1)
    before_dedup = len(df)
    df = df.drop_duplicates(subset=['row_hash'])
    metrics['dropped_duplicates'] = before_dedup - len(df)

    # 6. Validation of output schema
    try:
        CLEAN_SCHEMA.validate(df, lazy=True)
        logger.info("Validation of output schema: OK")
    except pa.errors.SchemaErrors as e:
        logger.error(f"Output schema errors: {e.failure_cases}")

    metrics['final_count'] = len(df)
    metrics['initial_count'] = initial_count

    logger.info(f"Concluded transformation:")
    logger.info(f"Initial lines:       {initial_count}")
    logger.info(f"Nulls removed:       {metrics['dropped_null']}")
    logger.info(f"Invalid dates:       {metrics['dropped_invalid_date']}")
    logger.info(f"Duplicates removed:  {metrics['dropped_duplicates']}")
    logger.info(f"Final lines:         {metrics['final_count']}")

    return df, metrics


if __name__ == '__main__':
    df_raw = pd.read_csv('data/raw_transactions.csv')
    df_clean, metrics = transform(df_raw)
    df_clean.to_csv('data/clean_transactions.csv', index=False)
    print(f"\nCategories after normalization: {sorted(df_clean['category'].unique())}")