from .reader import NFCReader


class AndroidNFCReader(NFCReader):

    def read_uid(self) -> str:
        raise NotImplementedError(
            "Android NFC reader not implemented yet."
        )