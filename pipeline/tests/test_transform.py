# pipeline/tests/test_transform.py

import pytest
import pandas as pd
from src.transform import transform, normalise_category


# --- normalisation unit tests ---

def test_normalise_uppercase():
    assert normalise_category('RESTAURANTES') == 'Restaurantes'

def test_normalise_with_trailing_space():
    assert normalise_category('lazer ') == 'Lazer'

def test_normalise_without_accent():
    assert normalise_category('saude') == 'Saúde'

def test_normalise_none_returns_outros():
    assert normalise_category(None) == 'Outros'

def test_normalise_empty_string_returns_outros():
    assert normalise_category('') == 'Outros'

def test_normalise_unknown_returns_outros():
    assert normalise_category('categoria_desconhecida') == 'Outros'

def test_normalise_investimentos():
    assert normalise_category('invest') == 'Investimentos'


# --- fixtures ---

@pytest.fixture
def clean_dataframe():
    return pd.DataFrame([
        {'description': 'Continente',  'amount': 45.50, 'type': 'DEBIT',  'category': 'Supermercado', 'transaction_date': '2024-01-10'},
        {'description': 'Salário',     'amount': 1500,  'type': 'CREDIT', 'category': 'Outros',       'transaction_date': '2024-01-01'},
        {'description': 'Uber',        'amount': 8.90,  'type': 'DEBIT',  'category': 'Transportes',  'transaction_date': '2024-01-15'},
    ])

@pytest.fixture
def dirty_dataframe():
    return pd.DataFrame([
        {'description': 'Continente',  'amount': 45.50, 'type': 'DEBIT',  'category': 'supermercado', 'transaction_date': '2024-01-10'},
        {'description': 'Continente',  'amount': 45.50, 'type': 'DEBIT',  'category': 'supermercado', 'transaction_date': '2024-01-10'},  # duplicado
        {'description': 'Netflix',     'amount': 12.99, 'type': 'DEBIT',  'category': 'LAZER ',       'transaction_date': '2024-01-05'},
        {'description': 'Salário',     'amount': 1500,  'type': 'CREDIT', 'category': None,            'transaction_date': '2024-01-01'},
        {'description': 'Farmácia',    'amount': 22.00, 'type': 'DEBIT',  'category': 'saude',        'transaction_date': 'data-invalida'},  # data inválida
    ])


# --- pipeline integration tests ---

def test_transform_removes_duplicates(dirty_dataframe):
    df_clean, metrics = transform(dirty_dataframe)
    assert metrics['dropped_duplicates'] == 1

def test_transform_normalises_categories(dirty_dataframe):
    df_clean, metrics = transform(dirty_dataframe)
    assert 'supermercado' not in df_clean['category'].values
    assert 'LAZER ' not in df_clean['category'].values

def test_transform_null_category_becomes_outros(dirty_dataframe):
    df_clean, metrics = transform(dirty_dataframe)
    assert 'Outros' in df_clean['category'].values

def test_transform_removes_invalid_dates(dirty_dataframe):
    df_clean, metrics = transform(dirty_dataframe)
    assert metrics['dropped_invalid_date'] == 1

def test_transform_output_sorted_by_date(clean_dataframe):
    df_clean, _ = transform(clean_dataframe)
    dates = df_clean['transaction_date'].tolist()
    assert dates == sorted(dates)

def test_transform_no_negative_amounts():
    df = pd.DataFrame([
        {'description': 'Teste', 'amount': -10.00, 'type': 'DEBIT', 'category': 'Outros', 'transaction_date': '2024-01-01'},
        {'description': 'Válido', 'amount': 25.00, 'type': 'DEBIT', 'category': 'Outros', 'transaction_date': '2024-01-02'},
    ])
    df_clean, _ = transform(df)
    assert (df_clean['amount'] > 0).all()

def test_transform_preserves_valid_data(clean_dataframe):
    df_clean, metrics = transform(clean_dataframe)
    assert metrics['final_count'] == 3
    assert metrics['dropped_duplicates'] == 0