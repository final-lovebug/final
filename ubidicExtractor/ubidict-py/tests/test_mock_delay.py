from unittest.mock import patch

from app.mock_delay import sleep_mock_delay


def test_sleep_mock_delay_uses_one_to_three_second_range_and_reports_milliseconds():
    with (
        patch("app.mock_delay.random.uniform", return_value=2.25) as random_uniform,
        patch("app.mock_delay.time.sleep") as sleep,
    ):
        elapsed_ms = sleep_mock_delay()

    random_uniform.assert_called_once_with(1.0, 3.0)
    sleep.assert_called_once_with(2.25)
    assert elapsed_ms == 2250
