import logging
import json
from cryptography.fernet import Fernet


class DecryptionDriver:
    """
    Class for decrypting data using Fernet encryption.

    Parameters:
        spark_session (pyspark.sql.SparkSession): The Spark session object.

    Attributes:
        spark (pyspark.sql.SparkSession): The Spark session object.
    """

    def __init__(self, spark_session):
        """
        Initializes the DecryptionDriver with the provided Spark session.

        Parameters:
            spark_session (pyspark.sql.SparkSession): The Spark session object.
        """
        self.spark = spark_session

    def decrypt_data(self, spark, encrypted_data):
        """
        Decrypts data using Fernet decryption and returns the decrypted data as a Spark DataFrame.

        Parameters:
            conf (dict): A dictionary containing file paths and configurations.
            encrypted_data (bytes): The encrypted data to be decrypted.

        Returns:
            pyspark.sql.DataFrame: The decrypted data as a Spark DataFrame.

        Raises:
            Exception: If an error occurs during the decryption process.
        """
        try:
            key_file = 'C:\\Users\\Prasad\\Pictures\\visa\\mykey.key'
            with open(key_file, 'rb') as mykey:
                key = mykey.read()

            # Create a Fernet object with the key
            f = Fernet(key)

            # Decrypt the data
            decrypted_data_str = f.decrypt(encrypted_data).decode()
            decrypted_data_list = [json.loads(line) for line in decrypted_data_str.split('\n') if line]

            return decrypted_data_list

        except Exception as e:
            logging.error(f"Error occurred during decryption: {str(e)}")
            raise