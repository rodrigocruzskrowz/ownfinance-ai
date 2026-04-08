# pipeline/src/generate_data.py

# This script generates a 12 months dataset of financial transactions 
# with intentional noise and duplicates to test the robustness of the data processing pipeline.

import pandas as pd
import random
from faker import Faker
from datetime import date, timedelta
import os

fake = Faker('pt_PT')
random.seed(42)

CATEGORIES = [
    'Supermercado', 'Restaurantes', 'Transportes', 'Saúde',
    'Lazer', 'Habitação', 'Serviços', 'Vestuário', 'Educação', 'Investimentos', 'Outros'
]

# Intentional noise — inconsistent categories that the pipeline will normalize
NOISY_CATEGORIES = CATEGORIES + [
    'supermercado', 'RESTAURANTES', 'transporte', 'saude',
    'lazer ', ' Habitação', 'servicos', None, None, 'invest', None
]

MERCHANT_TEMPLATES = {
    'Supermercado': ['Continente', 'Pingo Doce', 'Lidl', 'Aldi', 'Intermarché'],
    'Restaurantes': ['McDonald\'s', 'Burger King', 'Nando\'s', 'Pizza Hut', 'Café Central'],
    'Transportes': ['CP - Comboios', 'Uber', 'Bolt', 'Galp', 'BP'],
    'Saúde':        ['Farmácia Bem-Estar', 'Hospital CUF', 'Dentista Silva', 'Óptica Nova'],
    'Lazer':        ['Cinema NOS', 'Netflix', 'Spotify', 'Steam', 'FNAC'],
    'Habitação':    ['EDP', 'Galp Gás', 'Águas de Lisboa', 'Condomínio'],
    'Serviços':     ['NOS Comunicações', 'MEO', 'Vodafone', 'CTT'],
    'Vestuário':    ['Zara', 'H&M', 'Pull & Bear', 'Mango'],
    'Educação':     ['Udemy', 'Coursera', 'Fnac Livros', 'ISCTE'],
    'Investimentos': ['Banco Best', 'Revolut', 'Degiro', 'XTB'],
    'Outros':       ['Amazon', 'eBay', 'Transferência', 'Levantamento ATM'],
}

CREDIT_DESCRIPTIONS = ['Salário', 'Transferência recebida', 'Reembolso', 'Freelance']


def generate_transactions(num_months: int = 12) -> pd.DataFrame:
    rows = []
    start_date = date.today().replace(day=1) - timedelta(days=num_months * 30)

    for month_offset in range(num_months):
        current_month = start_date + timedelta(days=month_offset * 30)

        # Credit - salary at the beginning of the month, with intentional variability in amount and date
        rows.append({
            'description': 'Salário',
            'amount': round(random.uniform(1100, 1800), 2),
            'type': 'CREDIT',
            'category': 'Outros',
            'transaction_date': current_month.replace(day=random.randint(1, 5)).isoformat(),
        })

        # Debits - between 15 to 25 transactions per month, with intentional noise in categories and dates slightly out of order
        for _ in range(random.randint(15, 25)):
            clean_category = random.choice(CATEGORIES)
            noisy_category = random.choice(NOISY_CATEGORIES)  # Intentionally noisy category
            merchant = random.choice(MERCHANT_TEMPLATES[clean_category])

            # Disordered dates within the month to test sorting and handling of out-of-order data
            day = random.randint(1, 28)
            transaction_date = current_month.replace(day=day)

            rows.append({
                'description': merchant,
                'amount': round(random.uniform(2, 250), 2),
                'type': 'DEBIT',
                'category': noisy_category,
                'transaction_date': transaction_date.isoformat(),
            })

    df = pd.DataFrame(rows)

    # Duplicates - 3% duplicates to test deduplication logic
    duplicates = df.sample(frac=0.03, random_state=42)
    df = pd.concat([df, duplicates], ignore_index=True)

    # Shuffle the dataset to ensure duplicates and noise are not clustered together
    df = df.sample(frac=1, random_state=42).reset_index(drop=True)

    return df


if __name__ == '__main__':
    os.makedirs('data', exist_ok=True)
    df = generate_transactions(12)
    df.to_csv('data/raw_transactions.csv', index=False)
    print(f"Generated {len(df)} transactions in data/raw_transactions.csv")
    print(f"Unique categories (with noise): {sorted(df['category'].dropna().unique())}")