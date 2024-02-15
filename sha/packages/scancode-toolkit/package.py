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
#     spack install scancode-toolkit
#
# You can edit this file again by typing:
#
#     spack edit scancode-toolkit
#
# See the Spack documentation for more information on packaging.
# ----------------------------------------------------------------------------

from spack.package import *
import subprocess

class ScancodeToolkit(Package):
    """FIXME: Put a proper description of your package here."""

    # FIXME: Add a proper url for your package's homepage here.
    homepage = "https://www.example.com"
    url = "https://github.com/nexB/scancode-toolkit/releases/download/v32.0.8/scancode-toolkit-v32.0.8_py3.10-linux.tar.gz"

    version("32.0.8_py3.10", sha256="de18aa06934064bf4966c1d49253285ad0fcc91e3917f2f877b0f9e54e59380c")

    depends_on("python@3.10", type=("build", "run"))

    def setup_run_environment(self, env):
        """Add 'bin' to PATH."""
        env.prepend_path("PATH", join_path(self.prefix, 'scancode-toolkit-v32.0.8'))

    def post_install(self, spec, prefix):
        command = join_path(prefix, 'scancode-toolkit-v32.0.8', 'scancode')
        subprocess.run([command])

    def install(self, spec, prefix):
        spark_install_dir = join_path(prefix, 'scancode-toolkit-v32.0.8')
        install_tree(self.stage.source_path, spark_install_dir)        
        self.post_install(spec, prefix)
        
