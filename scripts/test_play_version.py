import unittest
from unittest.mock import call, patch

from check_play_version import check_version


class PlayVersionTest(unittest.TestCase):
    @patch("check_play_version.request")
    def test_new_code_checks_all_artifacts_and_discards_edit(self, request):
        request.side_effect = [
            {"id": "preview"},
            {"bundles": [{"versionCode": 2}]},
            {"apks": [{"versionCode": 3}]},
            {"tracks": [{"releases": [{"versionCodes": ["4"]}]}]},
            {},
        ]
        check_version(5)
        self.assertEqual(request.call_args_list, [
            call("POST", "/edits", {}),
            call("GET", "/edits/preview/bundles"),
            call("GET", "/edits/preview/apks"),
            call("GET", "/edits/preview/tracks"),
            call("DELETE", "/edits/preview"),
        ])

    @patch("check_play_version.request")
    def test_equal_or_older_codes_are_rejected_for_each_artifact_source(self, request):
        for bundles, apks, tracks in [
            ({"bundles": [{"versionCode": 7}]}, {}, {}),
            ({}, {"apks": [{"versionCode": 7}]}, {}),
            ({}, {}, {"tracks": [{"releases": [{"versionCodes": ["7"]}]}]}),
        ]:
            for version in (6, 7):
                with self.subTest(version=version, bundles=bundles, apks=apks, tracks=tracks):
                    request.reset_mock()
                    request.side_effect = [{"id": "preview"}, bundles, apks, tracks, {}]
                    with self.assertRaisesRegex(ValueError, "must be greater than 7"):
                        check_version(version)
                    request.assert_called_with("DELETE", "/edits/preview")

    @patch("check_play_version.request")
    def test_failed_read_still_discards_the_unpublished_edit(self, request):
        request.side_effect = [{"id": "preview"}, OSError("API unavailable"), {}]
        with self.assertRaisesRegex(OSError, "API unavailable"):
            check_version(3)
        request.assert_called_with("DELETE", "/edits/preview")


if __name__ == "__main__":
    unittest.main()
