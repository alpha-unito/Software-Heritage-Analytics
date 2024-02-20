# Copyright 2013-2023 Lawrence Livermore National Security, LLC and other
# Spack Project Developers. See the top-level COPYRIGHT file for details.
#
# SPDX-License-Identifier: (Apache-2.0 OR MIT)

# ----------------------------------------------------------------------------
# If you submit this package back to Spack as a pull request,
# please first remove this boilerplate and all FIXME comments.
#
# This is a template package file for Spack.  We've put "FIXME"
# next to all the things you'll want to change. Once you've handled
# them, you can save this file and test your package like this:
#
#     spack install class Spark332BinHadoop3Scala213(Package):
#
# You can edit this file again by typing:
#
#     spack edit class Spark332BinHadoop3Scala213(Package):
#
# See the Spack documentation for more information on packaging.
# ----------------------------------------------------------------------------

from spack.package import *


class Spark332BinHadoop3Scala213(Package):
    """spark-3.3.2-bin-hadoop3-scala2.13"""

    homepage = "https://github.com/alpha-unito/Software-Heritage-Analytics"
    url = "https://archive.apache.org/dist/spark/spark-3.3.2/spark-3.3.2-bin-hadoop3-scala2.13.tgz"

    version("3.3.2-bin-hadoop3-scala2.13",
            sha512="3ce800ca3e0569ccb8736e4fcdb8146ec6d3070da7622dcc9d0edbeb2dc9524224f3a082a70a0faff91306369a837caa13291a09f3ad0d2b0b51548365f90ead")

    depends_on("openjdk@11.0.17_8", type='run')

    def setup_run_environment(self, env):
        env.set("SPARK_HOME", join_path(self.prefix,
                'spark-3.3.2-bin-hadoop3-scala2.13'))

    def install(self, spec, prefix):
        spark_install_dir = join_path(
            prefix, 'spark-3.3.2-bin-hadoop3-scala2.13')
        install_tree(self.stage.source_path, spark_install_dir)
