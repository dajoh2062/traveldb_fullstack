import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { fetchCountries } from "../api/travelApi";

export default function useCountries() {
  const { t } = useTranslation();
  const [countries, setCountries] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [requestKey, setRequestKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    fetchCountries({ signal: controller.signal })
      .then(setCountries)
      .catch(() => {
        if (!controller.signal.aborted) {
          setCountries([]);
          setHasError(true);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });
    return () => controller.abort();
  }, [requestKey]);

  function retry() {
    setCountries([]);
    setHasError(false);
    setIsLoading(true);
    setRequestKey(value => value + 1);
  }

  return {
    countries,
    error: hasError ? t("errors.countryOptions") : "",
    isLoading,
    retry,
  };
}
